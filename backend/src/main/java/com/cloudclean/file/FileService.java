package com.cloudclean.file;

import com.cloudclean.dto.FileItemResponse;
import com.cloudclean.dto.PageResponse;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.repository.FileItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileItemRepository fileItemRepository;

    public FileService(FileItemRepository fileItemRepository) {
        this.fileItemRepository = fileItemRepository;
    }

    public PageResponse<FileItemResponse> getFiles(String userId, String category, int page, int size, String sortBy, String sortOrder) {
        ensureInitialFilesForUser(userId);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = switch (sortBy.toLowerCase()) {
            case "name" -> "name";
            case "date" -> "lastModified";
            case "score" -> "blurScore";
            default -> "sizeBytes";
        };

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(direction, sortProperty));
        Page<FileItem> filePage;

        if (category == null || "all".equalsIgnoreCase(category)) {
            filePage = fileItemRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
        } else {
            filePage = fileItemRepository.findByUserIdAndCategoryAndIsDeletedFalse(userId, category.toLowerCase(), pageable);
        }

        return PageResponse.of(filePage.map(FileItemResponse::new));
    }

    public PageResponse<FileItemResponse> searchFiles(String userId, String query, int page, int size) {
        ensureInitialFilesForUser(userId);

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "sizeBytes"));
        Page<FileItem> filePage = fileItemRepository.searchFiles(userId, query, pageable);
        return PageResponse.of(filePage.map(FileItemResponse::new));
    }

    public FileItemResponse getFileById(String userId, String fileId) {
        FileItem item = fileItemRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        return new FileItemResponse(item);
    }

    public void deleteFile(String userId, String fileId) {
        FileItem item = fileItemRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        item.setDeleted(true);
        item.setDeletedAt(Instant.now());
        fileItemRepository.save(item);
        log.info("Soft deleted file id: {} for user: {}", fileId, userId);
    }

    public int deleteFilesBatch(String userId, List<String> fileIds) {
        List<FileItem> files = fileItemRepository.findByIdInAndUserId(fileIds, userId);
        Instant now = Instant.now();
        for (FileItem file : files) {
            file.setDeleted(true);
            file.setDeletedAt(now);
        }
        fileItemRepository.saveAll(files);
        log.info("Soft deleted {} files for user: {}", files.size(), userId);
        return files.size();
    }

    public void ensureInitialFilesForUser(String userId) {
        if (fileItemRepository.countByUserIdAndIsDeletedFalse(userId) == 0) {
            log.info("Seeding realistic sample data file index for user: {}", userId);
            seedRealisticFiles(userId);
        }
    }

    private void seedRealisticFiles(String userId) {
        Instant now = Instant.now();

        // 1. Duplicate: Docker Desktop Installer
        FileItem f1 = new FileItem();
        f1.setUserId(userId);
        f1.setName("docker_desktop_installer_v4.28.exe");
        f1.setExtension("exe");
        f1.setFileType("application");
        f1.setCategory("duplicate");
        f1.setSizeBytes(684200000L); // ~652 MB
        f1.setStorageLocation("Local: C:/Users/rocky/Downloads");
        f1.setStorageType("local");
        f1.setLastModified(now.minusSeconds(3600 * 24 * 45));
        f1.setLastAccessed(now.minusSeconds(3600 * 24 * 45));
        f1.setDetectionReason("Exact SHA-256 match with file in D:/Installers");
        f1.setRecommendationStatus("Safe to Delete");
        f1.setHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

        FileItem f2 = new FileItem();
        f2.setUserId(userId);
        f2.setName("docker_desktop_installer_v4.28 (1).exe");
        f2.setExtension("exe");
        f2.setFileType("application");
        f2.setCategory("duplicate");
        f2.setSizeBytes(684200000L);
        f2.setStorageLocation("Local: D:/Installers");
        f2.setStorageType("local");
        f2.setLastModified(now.minusSeconds(3600 * 24 * 45));
        f2.setLastAccessed(now.minusSeconds(3600 * 24 * 45));
        f2.setDetectionReason("Canonical original copy");
        f2.setRecommendationStatus("Keep Original");
        f2.setHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

        // 2. Blurry: Burst shot
        FileItem f3 = new FileItem();
        f3.setUserId(userId);
        f3.setName("DSC_0492_raw_burst_shot.jpg");
        f3.setExtension("jpg");
        f3.setFileType("image");
        f3.setCategory("blurry");
        f3.setSizeBytes(18450000L); // ~17.6 MB
        f3.setStorageLocation("Local: C:/Users/rocky/Pictures/Imports");
        f3.setStorageType("local");
        f3.setLastModified(now.minusSeconds(3600 * 24 * 80));
        f3.setLastAccessed(now.minusSeconds(3600 * 24 * 79));
        f3.setDetectionReason("Blur score 0.38 (Threshold: 0.85). High motion blur detected.");
        f3.setRecommendationStatus("Review & Delete");
        f3.setBlurScore(0.38);
        f3.setPreviewUrl("https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=600&auto=format&fit=crop&q=40&blur=8");

        // 3. Large: 4K Presentation Video
        FileItem f4 = new FileItem();
        f4.setUserId(userId);
        f4.setName("client_presentation_recording_4k.mp4");
        f4.setExtension("mp4");
        f4.setFileType("video");
        f4.setCategory("large");
        f4.setSizeBytes(14800000000L); // ~13.78 GB
        f4.setStorageLocation("Local: C:/Users/rocky/Videos/Captures");
        f4.setStorageType("local");
        f4.setLastModified(now.minusSeconds(3600 * 24 * 160));
        f4.setLastAccessed(now.minusSeconds(3600 * 24 * 159));
        f4.setDetectionReason("File size exceeds 10 GB limit; unaccessed in 160+ days");
        f4.setRecommendationStatus("Recommended for Archival");

        // 4. Large: Ubuntu ISO
        FileItem f5 = new FileItem();
        f5.setUserId(userId);
        f5.setName("ubuntu-24.04-live-server-amd64.iso");
        f5.setExtension("iso");
        f5.setFileType("archive");
        f5.setCategory("large");
        f5.setSizeBytes(2800000000L); // ~2.6 GB
        f5.setStorageLocation("Local: C:/Users/rocky/Downloads");
        f5.setStorageType("local");
        f5.setLastModified(now.minusSeconds(3600 * 24 * 190));
        f5.setLastAccessed(now.minusSeconds(3600 * 24 * 190));
        f5.setDetectionReason("Disk image unaccessed for >190 days");
        f5.setRecommendationStatus("Safe to Delete");

        // 5. Inactive: Legacy archive
        FileItem f6 = new FileItem();
        f6.setUserId(userId);
        f6.setName("legacy_project_backup_2024.tar.gz");
        f6.setExtension("tar.gz");
        f6.setFileType("archive");
        f6.setCategory("inactive");
        f6.setSizeBytes(8400000000L); // ~7.82 GB
        f6.setStorageLocation("Local: D:/Projects/Archive");
        f6.setStorageType("local");
        f6.setLastModified(now.minusSeconds(3600 * 24 * 360));
        f6.setLastAccessed(now.minusSeconds(3600 * 24 * 358));
        f6.setDetectionReason("Unaccessed for 360+ days; zero modification activity");
        f6.setRecommendationStatus("Recommended for Archival");

        // 6. Blurry: receipt scan
        FileItem f7 = new FileItem();
        f7.setUserId(userId);
        f7.setName("receipt_scan_blurry_008.png");
        f7.setExtension("png");
        f7.setFileType("image");
        f7.setCategory("blurry");
        f7.setSizeBytes(8900000L);
        f7.setStorageLocation("Local: C:/Users/rocky/Documents/Receipts");
        f7.setStorageType("local");
        f7.setLastModified(now.minusSeconds(3600 * 24 * 120));
        f7.setLastAccessed(now.minusSeconds(3600 * 24 * 120));
        f7.setDetectionReason("Blur score 0.42 (Laplacian low-frequency variance). Text illegible.");
        f7.setRecommendationStatus("Review & Delete");
        f7.setBlurScore(0.42);
        f7.setPreviewUrl("https://images.unsplash.com/photo-1586075010923-2dd4570fb338?w=600&auto=format&fit=crop&q=40&blur=6");

        // 7. Duplicate: financial report
        FileItem f8 = new FileItem();
        f8.setUserId(userId);
        f8.setName("annual_financial_report_2023_v3.pdf");
        f8.setExtension("pdf");
        f8.setFileType("document");
        f8.setCategory("duplicate");
        f8.setSizeBytes(45000000L);
        f8.setStorageLocation("Local: C:/Users/rocky/Downloads");
        f8.setStorageType("local");
        f8.setLastModified(now.minusSeconds(3600 * 24 * 280));
        f8.setLastAccessed(now.minusSeconds(3600 * 24 * 280));
        f8.setDetectionReason("Exact duplicate of file in Documents/Finances/2023");
        f8.setRecommendationStatus("Safe to Delete");
        f8.setHash("f5a89b702...financial2023");

        // 8. Large: Machine learning checkpoint
        FileItem f9 = new FileItem();
        f9.setUserId(userId);
        f9.setName("machine_learning_model_weights_epoch_50.ckpt");
        f9.setExtension("ckpt");
        f9.setFileType("data");
        f9.setCategory("large");
        f9.setSizeBytes(12400000000L); // ~11.5 GB
        f9.setStorageLocation("Local: D:/Projects/ML_Runs");
        f9.setStorageType("local");
        f9.setLastModified(now.minusSeconds(3600 * 24 * 140));
        f9.setLastAccessed(now.minusSeconds(3600 * 24 * 139));
        f9.setDetectionReason("Intermediate checkpoint superseded by epoch 100 weights");
        f9.setRecommendationStatus("Safe to Delete");

        fileItemRepository.saveAll(List.of(f1, f2, f3, f4, f5, f6, f7, f8, f9));
    }
}
