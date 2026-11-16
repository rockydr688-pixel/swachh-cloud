package com.cloudclean.storage.agent;

import com.cloudclean.storage.provider.ListFilesOptions;
import com.cloudclean.storage.provider.ProviderFileMetadata;
import com.cloudclean.util.FileHashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Lightweight local computer agent service.
 * Performs secure, whitelisted directory traversal, metadata extraction,
 * SHA-256 calculation, and cancellation-aware scanning without exfiltrating private files.
 */
@Component
public class LocalAgentScanner {

    private static final Logger log = LoggerFactory.getLogger(LocalAgentScanner.class);

    // List of forbidden sensitive OS system paths to prevent accidental or malicious scanning
    private static final Set<String> SYSTEM_RESTRICTED_PREFIXES = Set.of(
            "C:\\Windows",
            "C:\\Program Files",
            "C:\\Program Files (x86)",
            "/etc",
            "/sys",
            "/proc",
            "/boot",
            "/dev",
            "/root"
    );

    public record ScanProgressReport(
            int filesScanned,
            int failedFiles,
            long totalBytes,
            String currentDirectory
    ) {}

    /**
     * Validates whether a requested path is safe and permitted for local scanning.
     */
    public boolean isPermittedPath(Path targetPath, List<String> approvedFolders) {
        if (targetPath == null || approvedFolders == null || approvedFolders.isEmpty()) {
            return false;
        }

        try {
            Path normalizedTarget = targetPath.toAbsolutePath().normalize();
            String targetStr = normalizedTarget.toString();

            // Block sensitive system roots
            for (String restricted : SYSTEM_RESTRICTED_PREFIXES) {
                Path restrictedPath = Paths.get(restricted);
                if (targetStr.equalsIgnoreCase(restrictedPath.toString()) ||
                        targetStr.toLowerCase().startsWith(restrictedPath.toString().toLowerCase() + File.separator)) {
                    log.warn("Security Alert: Attempted access to restricted system folder: {}", targetStr);
                    return false;
                }
            }

            // Ensure target is within at least one approved folder
            for (String approved : approvedFolders) {
                Path approvedNormalized = Paths.get(approved).toAbsolutePath().normalize();
                if (normalizedTarget.startsWith(approvedNormalized)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Path security validation failed for: {}", targetPath, e);
            return false;
        }

        return false;
    }

    /**
     * Recursively scans approved directories, extracting metadata and computing hashes.
     */
    public List<ProviderFileMetadata> scanDirectories(
            List<String> approvedFolders,
            ListFilesOptions options,
            BooleanSupplier cancellationCheck,
            Consumer<ScanProgressReport> progressListener
    ) {
        List<ProviderFileMetadata> discoveredFiles = new ArrayList<>();
        int[] counters = new int[]{0, 0}; // [scanned, failed]
        long[] bytesAccumulator = new long[]{0};

        if (approvedFolders == null || approvedFolders.isEmpty()) {
            log.warn("No approved folders provided for local scanning");
            return discoveredFiles;
        }

        for (String folderStr : approvedFolders) {
            if (cancellationCheck != null && cancellationCheck.getAsBoolean()) {
                log.info("Local scan cancelled before processing folder: {}", folderStr);
                break;
            }

            Path folderPath;
            try {
                folderPath = Paths.get(folderStr).toAbsolutePath().normalize();
            } catch (Exception ex) {
                log.error("Invalid path specification: {}", folderStr);
                counters[1]++;
                continue;
            }

            if (!Files.exists(folderPath)) {
                log.warn("Configured monitored path does not exist on host: {}", folderPath);
                counters[1]++;
                continue;
            }

            if (!Files.isDirectory(folderPath)) {
                log.warn("Configured monitored path is not a directory: {}", folderPath);
                counters[1]++;
                continue;
            }

            try {
                Files.walkFileTree(folderPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new FileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (cancellationCheck != null && cancellationCheck.getAsBoolean()) {
                            log.info("Local scan cancelled during directory walk: {}", dir);
                            return FileVisitResult.TERMINATE;
                        }

                        // Skip trash folder
                        if (dir.getFileName() != null && dir.getFileName().toString().equals(".cloudclean_trash")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        // Path traversal check
                        if (!isPermittedPath(dir, approvedFolders)) {
                            log.warn("Access skipped for non-permitted directory: {}", dir);
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        if (progressListener != null) {
                            progressListener.accept(new ScanProgressReport(counters[0], counters[1], bytesAccumulator[0], dir.toString()));
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (cancellationCheck != null && cancellationCheck.getAsBoolean()) {
                            log.info("Local scan cancelled at file: {}", file);
                            return FileVisitResult.TERMINATE;
                        }

                        try {
                            if (!attrs.isRegularFile()) {
                                return FileVisitResult.CONTINUE;
                            }

                            // Incremental scan check: filter by modifiedSince if present
                            if (options != null && options.getModifiedSince() != null) {
                                Instant fileModified = attrs.lastModifiedTime().toInstant();
                                if (fileModified.isBefore(options.getModifiedSince())) {
                                    return FileVisitResult.CONTINUE;
                                }
                            }

                            ProviderFileMetadata meta = extractFileMetadata(file, attrs);
                            discoveredFiles.add(meta);
                            counters[0]++;
                            bytesAccumulator[0] += meta.getSizeBytes();

                            if (counters[0] % 25 == 0 && progressListener != null) {
                                progressListener.accept(new ScanProgressReport(counters[0], counters[1], bytesAccumulator[0], file.getParent() != null ? file.getParent().toString() : ""));
                            }
                        } catch (Exception ex) {
                            counters[1]++;
                            log.warn("Failed to extract metadata for file {}: {}", file, ex.getMessage());
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        counters[1]++;
                        log.warn("Inaccessible file encountered (permission denied or locked): {} ({})", file, exc != null ? exc.getMessage() : "Unknown");
                        return FileVisitResult.CONTINUE; // Continue traversal gracefully
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        if (exc != null) {
                            counters[1]++;
                            log.warn("Error leaving directory {}: {}", dir, exc.getMessage());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.error("Fatal directory traversal error on {}: {}", folderPath, e.getMessage());
                counters[1]++;
            }
        }

        if (progressListener != null) {
            progressListener.accept(new ScanProgressReport(counters[0], counters[1], bytesAccumulator[0], "Scan finished"));
        }

        log.info("Local agent scan completed: {} files discovered, {} failed/inaccessible, {} total bytes",
                counters[0], counters[1], bytesAccumulator[0]);
        return discoveredFiles;
    }

    /**
     * Extracts strict metadata conforming to system requirements.
     * Guaranteed never to equate modified timestamp with last accessed timestamp.
     */
    public ProviderFileMetadata extractFileMetadata(Path filePath, BasicFileAttributes attrs) throws IOException {
        String fileName = filePath.getFileName().toString();
        long size = attrs.size();

        String ext = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            ext = fileName.substring(dotIndex + 1).toLowerCase();
        }

        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = determineFallbackMimeType(ext);
        }

        Instant createdTime = attrs.creationTime().toInstant();
        Instant modifiedTime = attrs.lastModifiedTime().toInstant();

        // Specific handling for genuine lastAccessedTime vs modifiedTime
        Instant accessTime = attrs.lastAccessTime().toInstant();
        boolean accessReliable = !accessTime.equals(modifiedTime) && !accessTime.equals(createdTime);

        // Streaming SHA-256 computation
        String sha256 = FileHashUtils.computeSha256(filePath);

        ProviderFileMetadata metadata = new ProviderFileMetadata(
                filePath.toAbsolutePath().normalize().toString(),
                fileName,
                size,
                "local"
        );

        metadata.setExtension(ext);
        metadata.setMimeType(mimeType);
        metadata.setCreatedTime(createdTime);
        metadata.setModifiedTime(modifiedTime);
        metadata.setLastAccessedTime(accessTime);
        metadata.setLastAccessedReliable(accessReliable);
        metadata.setSha256Hash(sha256);
        metadata.setStorageLocation(maskSensitivePath(filePath.toAbsolutePath().toString()));
        metadata.setDirectory(attrs.isDirectory());
        metadata.setTrashed(false);

        return metadata;
    }

    private String determineFallbackMimeType(String ext) {
        return switch (ext.toLowerCase()) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "mkv" -> "video/x-matroska";
            case "mov" -> "video/quicktime";
            case "pdf" -> "application/pdf";
            case "zip" -> "application/zip";
            case "tar", "gz" -> "application/gzip";
            case "txt" -> "text/plain";
            case "json" -> "application/json";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }

    /**
     * Sanitizes file paths before exposing them, replacing user home directory with a safe prefix.
     */
    public String maskSensitivePath(String rawPath) {
        if (rawPath == null) return "";
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank() && rawPath.startsWith(userHome)) {
            return "~" + rawPath.substring(userHome.length()).replace('\\', '/');
        }
        return rawPath.replace('\\', '/');
    }
}
