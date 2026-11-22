package com.cloudclean.storage.dropbox;

import com.cloudclean.exception.BadRequestException;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.repository.StorageConnectionRepository;
import com.cloudclean.storage.StorageConnection;
import com.cloudclean.storage.provider.ListFilesOptions;
import com.cloudclean.storage.provider.ProviderFileMetadata;
import com.cloudclean.storage.provider.StorageProvider;
import com.cloudclean.storage.provider.StorageUsage;
import com.cloudclean.util.EncryptionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component("dropboxAdapter")
public class DropboxAdapter implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(DropboxAdapter.class);

    private static final String DROPBOX_API_BASE = "https://api.dropboxapi.com/2";
    private static final String DROPBOX_CONTENT_BASE = "https://content.dropboxapi.com/2";

    private final StorageConnectionRepository connectionRepository;
    private final EncryptionUtils encryptionUtils;
    private final DropboxOAuthService dropboxOAuthService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DropboxAdapter(StorageConnectionRepository connectionRepository,
                          EncryptionUtils encryptionUtils,
                          DropboxOAuthService dropboxOAuthService,
                          ObjectMapper objectMapper) {
        this.connectionRepository = connectionRepository;
        this.encryptionUtils = encryptionUtils;
        this.dropboxOAuthService = dropboxOAuthService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    @Override
    public String getProviderType() {
        return "dropbox";
    }

    @Override
    public List<ProviderFileMetadata> listFiles(String connectionId, ListFilesOptions options) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        List<ProviderFileMetadata> results = new ArrayList<>();
        int maxFiles = (options != null && options.getPageSize() > 0) ? options.getPageSize() : 5000;

        try {
            String initialBody = "{\"path\": \"\", \"recursive\": true, \"include_media_info\": true, \"include_deleted\": false}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DROPBOX_API_BASE + "/files/list_folder"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(initialBody))
                    .build();

            HttpResponse<String> response = executeWithRetry(request, connection);
            if (response.statusCode() >= 400) {
                log.error("Dropbox list_folder error {}: {}", response.statusCode(), response.body());
                throw new BadRequestException("Dropbox file listing failed: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            processEntries(root.path("entries"), results, options, maxFiles);

            boolean hasMore = root.path("has_more").asBoolean(false);
            String cursor = root.path("cursor").asText(null);

            while (hasMore && cursor != null && results.size() < maxFiles) {
                String continueBody = "{\"cursor\": \"" + cursor + "\"}";
                HttpRequest contReq = HttpRequest.newBuilder()
                        .uri(URI.create(DROPBOX_API_BASE + "/files/list_folder/continue"))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(continueBody))
                        .build();

                HttpResponse<String> contResp = executeWithRetry(contReq, connection);
                if (contResp.statusCode() >= 400) break;

                JsonNode contRoot = objectMapper.readTree(contResp.body());
                processEntries(contRoot.path("entries"), results, options, maxFiles);

                hasMore = contRoot.path("has_more").asBoolean(false);
                cursor = contRoot.path("cursor").asText(null);
            }

            return results;
        } catch (Exception e) {
            log.error("Failed to list files from Dropbox for connection {}: {}", connectionId, e.getMessage());
            throw new BadRequestException("Failed to list files from Dropbox: " + e.getMessage());
        }
    }

    private void processEntries(JsonNode entries, List<ProviderFileMetadata> results, ListFilesOptions options, int maxFiles) {
        if (!entries.isArray()) return;

        for (JsonNode entry : entries) {
            if (results.size() >= maxFiles) break;
            String tag = entry.path(".tag").asText();
            if (!"file".equalsIgnoreCase(tag)) continue;

            ProviderFileMetadata meta = mapDropboxEntryToMetadata(entry);
            if (options != null && options.getModifiedSince() != null) {
                if (meta.getModifiedTime() != null && meta.getModifiedTime().isBefore(options.getModifiedSince())) {
                    continue;
                }
            }
            results.add(meta);
        }
    }

    @Override
    public ProviderFileMetadata getFileMetadata(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        try {
            String body = "{\"path\": \"" + providerFileId + "\", \"include_media_info\": true}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DROPBOX_API_BASE + "/files/get_metadata"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = executeWithRetry(request, connection);
            if (response.statusCode() == 409 || response.statusCode() == 404) {
                throw new ResourceNotFoundException("Dropbox file not found: " + providerFileId);
            }
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Dropbox error: " + response.body());
            }

            JsonNode entry = objectMapper.readTree(response.body());
            return mapDropboxEntryToMetadata(entry);
        } catch (ResourceNotFoundException | BadRequestException ex) {
            throw ex;
        } catch (Exception e) {
            throw new BadRequestException("Failed to fetch Dropbox file metadata: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        try {
            String argJson = "{\"path\": \"" + providerFileId + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DROPBOX_CONTENT_BASE + "/files/download"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Dropbox-API-Arg", argJson)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Failed to download file from Dropbox. HTTP status: " + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            log.error("Error downloading file {} from Dropbox: {}", providerFileId, e.getMessage());
            throw new BadRequestException("Failed to download file from Dropbox: " + e.getMessage());
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
            throw new BadRequestException("Error reading Dropbox file content: " + e.getMessage());
        }
    }

    @Override
    public boolean moveToTrash(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        try {
            String body = "{\"path\": \"" + providerFileId + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DROPBOX_API_BASE + "/files/delete_v2"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = executeWithRetry(request, connection);
            // In Dropbox, delete_v2 moves file to Dropbox deleted files retention bin (30-180 days rollback window)
            if (response.statusCode() == 200) {
                log.info("Successfully moved Dropbox file {} to deleted items retention", providerFileId);
                return true;
            }
            throw new BadRequestException("Dropbox delete_v2 failed: " + response.body());
        } catch (Exception e) {
            log.error("Error moving Dropbox file {} to trash: {}", providerFileId, e.getMessage());
            throw new BadRequestException("Failed to move Dropbox file to trash: " + e.getMessage());
        }
    }

    @Override
    public boolean restoreFile(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        try {
            String body = "{\"path\": \"" + providerFileId + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DROPBOX_API_BASE + "/files/restore"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = executeWithRetry(request, connection);
            if (response.statusCode() == 200) {
                log.info("Successfully restored Dropbox file {}", providerFileId);
                return true;
            }
            throw new BadRequestException("Dropbox restore failed: " + response.body());
        } catch (Exception e) {
            log.error("Error restoring Dropbox file {}: {}", providerFileId, e.getMessage());
            throw new BadRequestException("Failed to restore Dropbox file: " + e.getMessage());
        }
    }

    @Override
    public StorageUsage getStorageUsage(String connectionId) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DROPBOX_API_BASE + "/users/get_space_usage"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("null"))
                    .build();

            HttpResponse<String> response = executeWithRetry(request, connection);
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                long used = root.path("used").asLong(0L);
                long total = root.path("allocation").path("allocated").asLong(2147483648L); // default 2GB free
                long remaining = Math.max(0, total - used);

                return new StorageUsage(total, used, remaining, 0L);
            }
        } catch (Exception e) {
            log.warn("Could not retrieve Dropbox space usage: {}", e.getMessage());
        }
        return new StorageUsage(2147483648L, connection.getUsedBytes(), 2147483648L - connection.getUsedBytes());
    }

    private ProviderFileMetadata mapDropboxEntryToMetadata(JsonNode entry) {
        String id = entry.path("id").asText();
        String name = entry.path("name").asText("untitled");
        String pathDisplay = entry.path("path_display").asText("/" + name);
        long size = entry.path("size").asLong(0L);

        String extension = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            extension = name.substring(dot).toLowerCase();
        }

        String mimeType = guessMimeType(extension);

        Instant modified = null;
        if (entry.has("server_modified")) {
            try { modified = Instant.parse(entry.path("server_modified").asText()); } catch (Exception ignored) {}
        }

        Instant created = null;
        if (entry.has("client_modified")) {
            try { created = Instant.parse(entry.path("client_modified").asText()); } catch (Exception ignored) {}
        }

        String contentHash = entry.has("content_hash") ? entry.path("content_hash").asText() : null;

        ProviderFileMetadata meta = new ProviderFileMetadata();
        meta.setProviderFileId(id);
        meta.setName(name);
        meta.setExtension(extension);
        meta.setMimeType(mimeType);
        meta.setSizeBytes(size);
        meta.setStorageLocation("Dropbox:" + pathDisplay);
        meta.setStorageType("dropbox");
        meta.setCreatedTime(created != null ? created : modified);
        meta.setModifiedTime(modified != null ? modified : Instant.now());
        meta.setSha256Hash(contentHash);
        meta.setDirectory(false);
        meta.setTrashed(false);
        return meta;
    }

    private String guessMimeType(String ext) {
        return switch (ext.toLowerCase()) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".pdf" -> "application/pdf";
            case ".mp4" -> "video/mp4";
            case ".mov" -> "video/quicktime";
            case ".zip" -> "application/zip";
            case ".tar", ".gz" -> "application/gzip";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default -> "application/octet-stream";
        };
    }

    private HttpResponse<String> executeWithRetry(HttpRequest request, StorageConnection connection) throws Exception {
        int maxRetries = 2;
        int attempt = 0;

        while (attempt <= maxRetries) {
            attempt++;
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401 && attempt <= maxRetries) {
                log.warn("Dropbox access token expired (401), refreshing token...");
                String newAccessToken = dropboxOAuthService.refreshAccessToken(connection);
                connectionRepository.save(connection);

                HttpRequest.Builder retryBuilder = HttpRequest.newBuilder()
                        .uri(request.uri())
                        .method(request.method(), request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));

                request.headers().map().forEach((k, v) -> {
                    if (!"Authorization".equalsIgnoreCase(k)) {
                        v.forEach(val -> retryBuilder.header(k, val));
                    }
                });
                retryBuilder.header("Authorization", "Bearer " + newAccessToken);
                request = retryBuilder.build();
                continue;
            }

            if (response.statusCode() == 429 && attempt <= maxRetries) {
                long backoffSec = response.headers().firstValue("Retry-After").map(Long::parseLong).orElse(2L);
                log.warn("Dropbox rate limit reached (429), backing off for {} seconds...", backoffSec);
                Thread.sleep(Math.min(backoffSec, 10L) * 1000);
                continue;
            }

            return response;
        }

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private StorageConnection getConnection(String connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Storage connection not found: " + connectionId));
    }

    private String getValidAccessToken(StorageConnection connection) {
        if (connection.getEncryptedAccessToken() == null) {
            throw new BadRequestException("No access token found for Dropbox connection");
        }
        if (connection.getTokenExpiresAt() != null && connection.getTokenExpiresAt().isBefore(Instant.now().plusSeconds(60))) {
            return dropboxOAuthService.refreshAccessToken(connection);
        }
        return encryptionUtils.decrypt(connection.getEncryptedAccessToken());
    }
}
