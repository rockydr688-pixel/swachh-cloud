package com.cloudclean.detection;

import com.cloudclean.file.FileItem;
import com.cloudclean.repository.DuplicateGroupRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.storage.provider.StorageProvider;
import com.cloudclean.storage.provider.StorageProviderRegistry;
import com.cloudclean.util.FileHashUtils;
import com.cloudclean.util.PerceptualHashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DuplicateDetector {

    private static final Logger log = LoggerFactory.getLogger(DuplicateDetector.class);

    private final DuplicateGroupRepository duplicateGroupRepository;
    private final FileItemRepository fileItemRepository;
    private final StorageProviderRegistry storageProviderRegistry;

    public DuplicateDetector(DuplicateGroupRepository duplicateGroupRepository,
                             FileItemRepository fileItemRepository,
                             StorageProviderRegistry storageProviderRegistry) {
        this.duplicateGroupRepository = duplicateGroupRepository;
        this.fileItemRepository = fileItemRepository;
        this.storageProviderRegistry = storageProviderRegistry;
    }

    public record DuplicateDetectionReport(
            List<DuplicateGroup> duplicateGroups,
            int totalDuplicateFiles,
            long totalRecoverableBytes,
            int candidateCopiesCount
    ) {}

    public record SimilarImageGroup(
            String id,
            String primaryImageId,
            String perceptualHash,
            List<String> similarImageIds,
            int distance
    ) {}

    /**
     * Executes exact duplicate detection with size-first optimization and trustworthy hash caching.
     */
    public DuplicateDetectionReport detectDuplicates(String userId) {
        List<FileItem> allFiles = fileItemRepository.findByUserIdAndIsDeletedFalse(userId);

        // Step 1: Compare file sizes first before expensive hashing
        Map<Long, List<FileItem>> filesBySize = allFiles.stream()
                .filter(f -> f.getSizeBytes() > 0)
                .collect(Collectors.groupingBy(FileItem::getSizeBytes));

        // Step 2: Only compute/verify hashes for files that share identical sizes with at least one other file
        Map<String, List<FileItem>> hashGroups = new HashMap<>();

        for (Map.Entry<Long, List<FileItem>> sizeEntry : filesBySize.entrySet()) {
            List<FileItem> sameSizeFiles = sizeEntry.getValue();
            if (sameSizeFiles.size() < 2) {
                // Size is unique -> cannot be a duplicate; skip expensive hashing
                continue;
            }

            for (FileItem file : sameSizeFiles) {
                String sha256 = file.getHash();

                // Step 3: Avoid repeated hashing when a trustworthy cached hash exists
                if (sha256 == null || sha256.isBlank()) {
                    sha256 = resolveFileSha256(file);
                    if (sha256 != null && !sha256.isBlank()) {
                        file.setHash(sha256);
                    }
                }

                if (sha256 != null && !sha256.isBlank()) {
                    hashGroups.computeIfAbsent(sha256, k -> new ArrayList<>()).add(file);
                }
            }
        }

        // Step 4: Build duplicate groups and identify canonical original vs candidate copies
        duplicateGroupRepository.deleteByUserId(userId);
        List<DuplicateGroup> duplicateGroups = new ArrayList<>();
        int totalDuplicateFiles = 0;
        long totalRecoverableBytes = 0L;

        for (Map.Entry<String, List<FileItem>> entry : hashGroups.entrySet()) {
            List<FileItem> groupFiles = entry.getValue();
            if (groupFiles.size() > 1) {
                // Determine canonical original (e.g. earliest created / modified date, shortest path)
                FileItem original = selectCanonicalOriginal(groupFiles);
                DuplicateGroup group = new DuplicateGroup(userId, entry.getKey(), original.getSizeBytes(), original.getId());

                for (FileItem file : groupFiles) {
                    if (file.getId().equals(original.getId())) {
                        file.setCategory("duplicate");
                        file.setRecommendationStatus("Keep Original");
                        file.setDetectionReason("Canonical master copy (earliest verified)");
                    } else {
                        group.addDuplicate(file.getId());
                        file.setCategory("duplicate");
                        file.setRecommendationStatus("Safe to Delete");
                        file.setDetectionReason("Exact SHA-256 clone of canonical original: " + original.getName());
                    }
                }

                duplicateGroups.add(group);
                totalDuplicateFiles += group.getDuplicateFileIds().size();
                totalRecoverableBytes += group.getRecoverableBytes(); // Calculated without double-counting
            }
        }

        fileItemRepository.saveAll(allFiles);
        List<DuplicateGroup> savedGroups = duplicateGroupRepository.saveAll(duplicateGroups);

        log.info("Duplicate detection completed for user {}: {} groups, {} duplicate copies, {} recoverable bytes",
                userId, savedGroups.size(), totalDuplicateFiles, totalRecoverableBytes);

        return new DuplicateDetectionReport(savedGroups, totalDuplicateFiles, totalRecoverableBytes, totalDuplicateFiles);
    }

    /**
     * Perceptual hashing for visually similar images as a SEPARATE feature.
     * Does not confuse visually similar images with identical files.
     */
    public List<SimilarImageGroup> detectVisuallySimilarImages(String userId, int maxHammingDistance) {
        List<FileItem> imageFiles = fileItemRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                .filter(f -> "image".equalsIgnoreCase(f.getFileType()) || (f.getMimeType() != null && f.getMimeType().startsWith("image/")))
                .collect(Collectors.toList());

        // Ensure perceptual hash is computed for each image
        for (FileItem img : imageFiles) {
            if (img.getPerceptualHash() == null || img.getPerceptualHash().isBlank()) {
                String dhash = resolvePerceptualHash(img);
                if (dhash != null) {
                    img.setPerceptualHash(dhash);
                }
            }
        }
        fileItemRepository.saveAll(imageFiles);

        List<SimilarImageGroup> similarGroups = new ArrayList<>();
        Set<String> groupedIds = new HashSet<>();

        for (int i = 0; i < imageFiles.size(); i++) {
            FileItem imgA = imageFiles.get(i);
            if (imgA.getPerceptualHash() == null || groupedIds.contains(imgA.getId())) continue;

            List<String> matches = new ArrayList<>();
            int minDistance = 64;

            for (int j = i + 1; j < imageFiles.size(); j++) {
                FileItem imgB = imageFiles.get(j);
                if (imgB.getPerceptualHash() == null || groupedIds.contains(imgB.getId())) continue;

                // Compare perceptual hashes (dHash Hamming distance)
                int dist = PerceptualHashUtils.calculateHammingDistance(imgA.getPerceptualHash(), imgB.getPerceptualHash());
                if (dist <= maxHammingDistance) {
                    matches.add(imgB.getId());
                    groupedIds.add(imgB.getId());
                    minDistance = Math.min(minDistance, dist);
                }
            }

            if (!matches.isEmpty()) {
                groupedIds.add(imgA.getId());
                SimilarImageGroup group = new SimilarImageGroup(
                        UUID.randomUUID().toString(),
                        imgA.getId(),
                        imgA.getPerceptualHash(),
                        matches,
                        minDistance
                );
                similarGroups.add(group);
            }
        }

        log.info("Perceptual similarity detection found {} clusters of visually similar images for user {}",
                similarGroups.size(), userId);
        return similarGroups;
    }

    private FileItem selectCanonicalOriginal(List<FileItem> files) {
        // Sort by createdTime ascending, then modifiedTime ascending, then shortest location
        return files.stream()
                .min(Comparator.comparing(
                                (FileItem f) -> f.getCreatedTime() != null ? f.getCreatedTime() : f.getLastModified(),
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(
                                (FileItem f) -> f.getStorageLocation() != null ? f.getStorageLocation().length() : 9999
                        ))
                .orElse(files.get(0));
    }

    private String resolveFileSha256(FileItem file) {
        // Try local file path first
        if (file.getStorageLocation() != null) {
            try {
                Path localPath = Paths.get(file.getStorageLocation());
                if (Files.exists(localPath) && Files.isRegularFile(localPath)) {
                    return FileHashUtils.computeSha256(localPath);
                }
            } catch (Exception ignored) {}
        }

        // Try storage provider stream if connected
        if (file.getStorageType() != null && file.getStorageConnectionId() != null && file.getProviderFileId() != null) {
            try {
                if (storageProviderRegistry.hasProvider(file.getStorageType())) {
                    StorageProvider provider = storageProviderRegistry.getProvider(file.getStorageType());
                    try (InputStream is = provider.downloadFile(file.getStorageConnectionId(), file.getProviderFileId())) {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) > 0) {
                            digest.update(buffer, 0, read);
                        }
                        return HexFormat.of().formatHex(digest.digest());
                    }
                }
            } catch (Exception e) {
                log.debug("Could not stream hash for file {}: {}", file.getId(), e.getMessage());
            }
        }

        return null;
    }

    private String resolvePerceptualHash(FileItem file) {
        if (file.getStorageLocation() != null) {
            try {
                Path path = Paths.get(file.getStorageLocation());
                if (Files.exists(path)) {
                    return PerceptualHashUtils.computeDHash(path);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
