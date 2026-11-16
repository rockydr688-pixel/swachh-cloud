package com.cloudclean.storage.provider;

import com.cloudclean.exception.BadRequestException;
import com.cloudclean.exception.ForbiddenException;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.repository.StorageConnectionRepository;
import com.cloudclean.storage.StorageConnection;
import com.cloudclean.storage.agent.LocalAgentScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@Component("localStorageAdapter")
public class LocalStorageAdapter implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageAdapter.class);

    private final StorageConnectionRepository connectionRepository;
    private final LocalAgentScanner localAgentScanner;

    public LocalStorageAdapter(StorageConnectionRepository connectionRepository,
                               LocalAgentScanner localAgentScanner) {
        this.connectionRepository = connectionRepository;
        this.localAgentScanner = localAgentScanner;
    }

    @Override
    public String getProviderType() {
        return "local";
    }

    @Override
    public List<ProviderFileMetadata> listFiles(String connectionId, ListFilesOptions options) {
        StorageConnection connection = getConnection(connectionId);
        List<String> monitoredPaths = connection.getMonitoredPaths();

        if (monitoredPaths == null || monitoredPaths.isEmpty()) {
            log.warn("Storage connection {} has no monitored paths configured", connectionId);
            return List.of();
        }

        // If specific target paths requested, filter only to approved ones
        List<String> effectivePaths = new ArrayList<>();
        if (options != null && options.getTargetPaths() != null && !options.getTargetPaths().isEmpty()) {
            for (String target : options.getTargetPaths()) {
                Path targetPath = Paths.get(target);
                if (localAgentScanner.isPermittedPath(targetPath, monitoredPaths)) {
                    effectivePaths.add(target);
                } else {
                    log.warn("Requested path is not permitted under approved paths: {}", target);
                }
            }
        } else {
            effectivePaths.addAll(monitoredPaths);
        }

        return localAgentScanner.scanDirectories(
                effectivePaths,
                options,
                () -> false, // default no cancellation in synchronous listing
                null
        );
    }

    @Override
    public ProviderFileMetadata getFileMetadata(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        Path targetPath = Paths.get(providerFileId).toAbsolutePath().normalize();

        if (!localAgentScanner.isPermittedPath(targetPath, connection.getMonitoredPaths())) {
            throw new ForbiddenException("Access to path is forbidden or outside registered directories");
        }

        if (!Files.exists(targetPath)) {
            throw new ResourceNotFoundException("Local file not found: " + targetPath.getFileName());
        }

        try {
            BasicFileAttributes attrs = Files.readAttributes(targetPath, BasicFileAttributes.class);
            return localAgentScanner.extractFileMetadata(targetPath, attrs);
        } catch (IOException e) {
            throw new BadRequestException("Failed to read file attributes: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        Path targetPath = Paths.get(providerFileId).toAbsolutePath().normalize();

        if (!localAgentScanner.isPermittedPath(targetPath, connection.getMonitoredPaths())) {
            throw new ForbiddenException("Access to path is forbidden");
        }

        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new ResourceNotFoundException("File not found or is a directory: " + targetPath);
        }

        try {
            return Files.newInputStream(targetPath, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new BadRequestException("Cannot open file stream: " + e.getMessage());
        }
    }

    @Override
    public byte[] getFileContent(String connectionId, String providerFileId, int maxBytes) {
        try (InputStream is = downloadFile(connectionId, providerFileId)) {
            byte[] buffer = new byte[Math.min(maxBytes, 1024 * 1024 * 10)]; // max 10MB safety cap
            int bytesRead = is.read(buffer);
            if (bytesRead <= 0) {
                return new byte[0];
            }
            if (bytesRead < buffer.length) {
                byte[] trimmed = new byte[bytesRead];
                System.arraycopy(buffer, 0, trimmed, 0, bytesRead);
                return trimmed;
            }
            return buffer;
        } catch (IOException e) {
            throw new BadRequestException("Error reading file content: " + e.getMessage());
        }
    }

    @Override
    public boolean moveToTrash(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        Path sourcePath = Paths.get(providerFileId).toAbsolutePath().normalize();

        if (!localAgentScanner.isPermittedPath(sourcePath, connection.getMonitoredPaths())) {
            throw new ForbiddenException("Cannot trash file outside monitored paths");
        }

        if (!Files.exists(sourcePath)) {
            throw new ResourceNotFoundException("File to trash not found: " + sourcePath);
        }

        try {
            // Find the root approved folder containing this file
            Path rootApproved = findRootApprovedFolder(sourcePath, connection.getMonitoredPaths());
            Path trashDir = rootApproved.resolve(".cloudclean_trash");
            if (!Files.exists(trashDir)) {
                Files.createDirectories(trashDir);
            }

            // Create relative path hierarchy inside trash to prevent name collisions and enable perfect restore
            Path relativePath = rootApproved.relativize(sourcePath);
            Path destinationInTrash = trashDir.resolve(relativePath);
            if (destinationInTrash.getParent() != null && !Files.exists(destinationInTrash.getParent())) {
                Files.createDirectories(destinationInTrash.getParent());
            }

            // If a file already exists in trash at destination, rename with timestamp
            if (Files.exists(destinationInTrash)) {
                String timestamp = String.valueOf(System.currentTimeMillis());
                destinationInTrash = trashDir.resolve(relativePath.toString() + "." + timestamp + ".bak");
            }

            Files.move(sourcePath, destinationInTrash, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            log.info("Safely relocated local file to trash sandbox: {} -> {}", sourcePath, destinationInTrash);
            return true;
        } catch (Exception e) {
            log.error("Failed to move file to trash: {}", sourcePath, e);
            throw new BadRequestException("Could not safely move file to trash: " + e.getMessage());
        }
    }

    @Override
    public boolean restoreFile(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        Path originalPath = Paths.get(providerFileId).toAbsolutePath().normalize();

        Path rootApproved = findRootApprovedFolder(originalPath, connection.getMonitoredPaths());
        Path trashDir = rootApproved.resolve(".cloudclean_trash");
        Path relativePath = rootApproved.relativize(originalPath);
        Path trashedFilePath = trashDir.resolve(relativePath);

        if (!Files.exists(trashedFilePath)) {
            throw new ResourceNotFoundException("Trashed file not found in local trash staging: " + trashedFilePath);
        }

        try {
            if (originalPath.getParent() != null && !Files.exists(originalPath.getParent())) {
                Files.createDirectories(originalPath.getParent());
            }

            Files.move(trashedFilePath, originalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            log.info("Restored local file from trash: {} -> {}", trashedFilePath, originalPath);
            return true;
        } catch (IOException e) {
            log.error("Failed to restore file from trash: {}", trashedFilePath, e);
            throw new BadRequestException("Could not restore file from trash: " + e.getMessage());
        }
    }

    @Override
    public StorageUsage getStorageUsage(String connectionId) {
        StorageConnection connection = getConnection(connectionId);
        List<String> paths = connection.getMonitoredPaths();

        long totalCapacity = 0;
        long usableSpace = 0;

        if (paths != null && !paths.isEmpty()) {
            for (String p : paths) {
                try {
                    Path path = Paths.get(p);
                    if (Files.exists(path)) {
                        FileStore store = Files.getFileStore(path);
                        totalCapacity = Math.max(totalCapacity, store.getTotalSpace());
                        usableSpace = Math.max(usableSpace, store.getUsableSpace());
                        break; // Get host drive capacity
                    }
                } catch (Exception ignored) {}
            }
        }

        if (totalCapacity == 0) {
            totalCapacity = connection.getTotalCapacityBytes() > 0 ? connection.getTotalCapacityBytes() : 1099511627776L;
            usableSpace = totalCapacity - connection.getUsedBytes();
        }

        long usedBytes = totalCapacity - usableSpace;
        return new StorageUsage(totalCapacity, usedBytes, usableSpace);
    }

    private StorageConnection getConnection(String connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Storage connection not found with id: " + connectionId));
    }

    private Path findRootApprovedFolder(Path target, List<String> approvedFolders) {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        for (String approved : approvedFolders) {
            Path app = Paths.get(approved).toAbsolutePath().normalize();
            if (normalizedTarget.startsWith(app)) {
                return app;
            }
        }
        throw new ForbiddenException("File path does not belong to any monitored folder: " + target);
    }
}
