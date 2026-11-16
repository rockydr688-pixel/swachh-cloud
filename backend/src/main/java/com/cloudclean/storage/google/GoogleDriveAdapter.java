package com.cloudclean.storage.google;

import com.cloudclean.exception.BadRequestException;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.exception.UnauthorizedException;
import com.cloudclean.repository.StorageConnectionRepository;
import com.cloudclean.storage.StorageConnection;
import com.cloudclean.storage.provider.ListFilesOptions;
import com.cloudclean.storage.provider.ProviderFileMetadata;
import com.cloudclean.storage.provider.StorageProvider;
import com.cloudclean.storage.provider.StorageUsage;
import com.cloudclean.util.EncryptionUtils;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component("googleDriveAdapter")
public class GoogleDriveAdapter implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveAdapter.class);

    private final StorageConnectionRepository connectionRepository;
    private final EncryptionUtils encryptionUtils;
    private final GoogleOAuthService googleOAuthService;

    public GoogleDriveAdapter(StorageConnectionRepository connectionRepository,
                              EncryptionUtils encryptionUtils,
                              GoogleOAuthService googleOAuthService) {
        this.connectionRepository = connectionRepository;
        this.encryptionUtils = encryptionUtils;
        this.googleOAuthService = googleOAuthService;
    }

    @Override
    public String getProviderType() {
        return "google_drive";
    }

    @Override
    public List<ProviderFileMetadata> listFiles(String connectionId, ListFilesOptions options) {
        StorageConnection connection = getConnection(connectionId);
        Drive drive = buildDriveClient(connection);

        List<ProviderFileMetadata> results = new ArrayList<>();

        try {
            StringBuilder queryBuilder = new StringBuilder();
            if (options != null && options.isIncludeTrashed()) {
                queryBuilder.append("trashed = true");
            } else {
                queryBuilder.append("trashed = false");
            }

            // Incremental scan support
            if (options != null && options.getModifiedSince() != null) {
                queryBuilder.append(" and modifiedTime > '").append(options.getModifiedSince().toString()).append("'");
            }

            if (options != null && options.getMimeTypeFilter() != null && !options.getMimeTypeFilter().isBlank()) {
                queryBuilder.append(" and mimeType = '").append(options.getMimeTypeFilter()).append("'");
            }

            Drive.Files.List request = drive.files().list()
                    .setQ(queryBuilder.toString())
                    .setFields("nextPageToken, files(id, name, mimeType, size, createdTime, modifiedTime, viewedByMeTime, trashed, webViewLink, thumbnailLink, md5Checksum, fileExtension)")
                    .setPageSize(options != null && options.getPageSize() > 0 ? Math.min(options.getPageSize(), 200) : 100);

            if (options != null && options.getPageToken() != null && !options.getPageToken().isBlank()) {
                request.setPageToken(options.getPageToken());
            }

            FileList fileList = request.execute();
            if (fileList.getFiles() != null) {
                for (File file : fileList.getFiles()) {
                    results.add(mapGoogleFileToMetadata(file));
                }
            }

            return results;
        } catch (IOException e) {
            log.error("Failed to list files from Google Drive for connection {}: {}", connectionId, e.getMessage());
            throw new BadRequestException("Failed to list files from Google Drive: " + e.getMessage());
        }
    }

    @Override
    public ProviderFileMetadata getFileMetadata(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        Drive drive = buildDriveClient(connection);

        try {
            File file = drive.files().get(providerFileId)
                    .setFields("id, name, mimeType, size, createdTime, modifiedTime, viewedByMeTime, trashed, webViewLink, thumbnailLink, md5Checksum, fileExtension")
                    .execute();
            return mapGoogleFileToMetadata(file);
        } catch (IOException e) {
            log.error("Failed to get Google Drive file metadata {}: {}", providerFileId, e.getMessage());
            throw new ResourceNotFoundException("Google Drive file not found: " + providerFileId);
        }
    }

    @Override
    public InputStream downloadFile(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        Drive drive = buildDriveClient(connection);

        try {
            return drive.files().get(providerFileId).executeMediaAsInputStream();
        } catch (IOException e) {
            log.error("Failed to download file {} from Google Drive: {}", providerFileId, e.getMessage());
            throw new BadRequestException("Failed to download file from Google Drive: " + e.getMessage());
        }
    }

    @Override
    public byte[] getFileContent(String connectionId, String providerFileId, int maxBytes) {
        try (InputStream is = downloadFile(connectionId, providerFileId)) {
            byte[] buffer = new byte[Math.min(maxBytes, 1024 * 1024 * 10)]; // 10MB safety bound
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
            throw new BadRequestException("Error reading Google Drive file content: " + e.getMessage());
        }
    }

    @Override
    public boolean moveToTrash(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        Drive drive = buildDriveClient(connection);

        try {
            File patch = new File().setTrashed(true);
            drive.files().update(providerFileId, patch).execute();
            log.info("Successfully trashed Google Drive file id: {}", providerFileId);
            return true;
        } catch (IOException e) {
            log.error("Error moving Google Drive file {} to trash: {}", providerFileId, e.getMessage());
            throw new BadRequestException("Error moving Google Drive file to trash: " + e.getMessage());
        }
    }

    @Override
    public boolean restoreFile(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        Drive drive = buildDriveClient(connection);

        try {
            File patch = new File().setTrashed(false);
            drive.files().update(providerFileId, patch).execute();
            log.info("Successfully restored Google Drive file id: {}", providerFileId);
            return true;
        } catch (IOException e) {
            log.error("Error restoring Google Drive file {}: {}", providerFileId, e.getMessage());
            throw new BadRequestException("Error restoring Google Drive file: " + e.getMessage());
        }
    }

    @Override
    public StorageUsage getStorageUsage(String connectionId) {
        StorageConnection connection = getConnection(connectionId);
        Drive drive = buildDriveClient(connection);

        try {
            About about = drive.about().get().setFields("storageQuota").execute();
            About.StorageQuota quota = about.getStorageQuota();

            long total = quota.getLimit() != null ? quota.getLimit() : 16106127360L; // default 15GB if unlimited
            long used = quota.getUsage() != null ? quota.getUsage() : 0L;
            long trash = quota.getUsageInDriveTrash() != null ? quota.getUsageInDriveTrash() : 0L;
            long available = Math.max(0, total - used);

            return new StorageUsage(total, used, available, trash);
        } catch (IOException e) {
            log.warn("Could not query Google Drive storage quota: {}", e.getMessage());
            return new StorageUsage(16106127360L, connection.getUsedBytes(), 16106127360L - connection.getUsedBytes());
        }
    }

    public Drive buildDriveClient(StorageConnection connection) {
        if (connection == null || connection.getEncryptedAccessToken() == null) {
            throw new UnauthorizedException("Google Drive is not connected or authorization token is missing");
        }

        // Check if access token is expired or close to expiring (within 60 seconds)
        Instant expiresAt = connection.getTokenExpiresAt();
        String accessToken;
        if (expiresAt != null && expiresAt.isBefore(Instant.now().plusSeconds(60))) {
            log.info("Google Drive token is expired/expiring soon for connection {}. Auto-refreshing...", connection.getId());
            accessToken = googleOAuthService.refreshAccessToken(connection);
            connectionRepository.save(connection);
        } else {
            accessToken = encryptionUtils.decrypt(connection.getEncryptedAccessToken());
        }

        HttpRequestInitializer requestInitializer = httpRequest -> {
            httpRequest.getHeaders().setAuthorization("Bearer " + accessToken);
            httpRequest.setConnectTimeout(10000);
            httpRequest.setReadTimeout(30000);
        };

        return new Drive.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), requestInitializer)
                .setApplicationName("CloudClean-AI")
                .build();
    }

    private StorageConnection getConnection(String connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Storage connection not found with id: " + connectionId));
    }

    private ProviderFileMetadata mapGoogleFileToMetadata(File file) {
        ProviderFileMetadata metadata = new ProviderFileMetadata();
        metadata.setProviderFileId(file.getId());
        metadata.setName(file.getName());
        metadata.setMimeType(file.getMimeType());
        metadata.setStorageType("google_drive");
        metadata.setStorageLocation("Google Drive / " + file.getName());
        metadata.setWebViewLink(file.getWebViewLink());
        metadata.setThumbnailLink(file.getThumbnailLink());
        metadata.setMd5Checksum(file.getMd5Checksum());
        metadata.setTrashed(Boolean.TRUE.equals(file.getTrashed()));

        // Determine file extension
        String ext = file.getFileExtension();
        if (ext == null || ext.isBlank()) {
            String name = file.getName();
            int dot = name != null ? name.lastIndexOf('.') : -1;
            if (dot > 0 && dot < name.length() - 1) {
                ext = name.substring(dot + 1).toLowerCase();
            }
        }
        metadata.setExtension(ext);

        // Size handling: native Google Docs/Sheets/Slides have null size
        if (file.getSize() != null) {
            metadata.setSizeBytes(file.getSize());
        } else {
            metadata.setSizeBytes(0L);
        }

        // Timestamps
        if (file.getCreatedTime() != null) {
            metadata.setCreatedTime(Instant.ofEpochMilli(file.getCreatedTime().getValue()));
        }
        if (file.getModifiedTime() != null) {
            metadata.setModifiedTime(Instant.ofEpochMilli(file.getModifiedTime().getValue()));
        }

        // Genuine last accessed time: Google Drive has viewedByMeTime
        if (file.getViewedByMeTime() != null) {
            metadata.setLastAccessedTime(Instant.ofEpochMilli(file.getViewedByMeTime().getValue()));
            metadata.setLastAccessedReliable(true);
        } else {
            // NEVER equate modifiedTime with lastAccessedTime
            metadata.setLastAccessedTime(null);
            metadata.setLastAccessedReliable(false);
        }

        // Is directory check
        boolean isFolder = "application/vnd.google-apps.folder".equals(file.getMimeType());
        metadata.setDirectory(isFolder);

        return metadata;
    }
}

