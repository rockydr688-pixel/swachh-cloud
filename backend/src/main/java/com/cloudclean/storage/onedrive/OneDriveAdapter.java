package com.cloudclean.storage.onedrive;

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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component("oneDriveAdapter")
public class OneDriveAdapter implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(OneDriveAdapter.class);

    private static final String GRAPH_BASE = "https://graph.microsoft.com/v1.0";

    private final StorageConnectionRepository connectionRepository;
    private final EncryptionUtils encryptionUtils;
    private final OneDriveOAuthService oneDriveOAuthService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OneDriveAdapter(StorageConnectionRepository connectionRepository,
                           EncryptionUtils encryptionUtils,
                           OneDriveOAuthService oneDriveOAuthService,
                           ObjectMapper objectMapper) {
        this.connectionRepository = connectionRepository;
        this.encryptionUtils = encryptionUtils;
        this.oneDriveOAuthService = oneDriveOAuthService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    @Override
    public String getProviderType() {
        return "onedrive";
    }

    @Override
    public List<ProviderFileMetadata> listFiles(String connectionId, ListFilesOptions options) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        List<ProviderFileMetadata> results = new ArrayList<>();
        Deque<String> folderQueue = new ArrayDeque<>();
        folderQueue.add("/me/drive/root/children");

        int maxFiles = (options != null && options.getPageSize() > 0) ? options.getPageSize() : 5000;

        try {
            while (!folderQueue.isEmpty() && results.size() < maxFiles) {
                String endpoint = folderQueue.poll();
                String nextLink = GRAPH_BASE + endpoint;

                while (nextLink != null && results.size() < maxFiles) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(nextLink))
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Accept", "application/json")
                            .GET()
                            .build();

                    HttpResponse<String> response = executeWithRetry(request, connection);
                    if (response.statusCode() >= 400) {
                        log.error("Microsoft Graph listFiles error: {} {}", response.statusCode(), response.body());
                        break;
                    }

                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode value = root.path("value");

                    if (value.isArray()) {
                        for (JsonNode item : value) {
                            boolean isFolder = item.has("folder");
                            String itemId = item.path("id").asText();

                            if (isFolder) {
                                if (options == null || options.isRecursive()) {
                                    folderQueue.add("/me/drive/items/" + itemId + "/children");
                                }
                            } else {
                                ProviderFileMetadata meta = mapDriveItemToMetadata(item);
                                if (options != null && options.getModifiedSince() != null) {
                                    if (meta.getModifiedTime() != null && meta.getModifiedTime().isBefore(options.getModifiedSince())) {
                                        continue;
                                    }
                                }
                                results.add(meta);
                            }
                        }
                    }

                    nextLink = root.has("@odata.nextLink") ? root.path("@odata.nextLink").asText() : null;
                }
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to list files from Microsoft OneDrive for connection {}: {}", connectionId, e.getMessage());
            throw new BadRequestException("Failed to list files from OneDrive: " + e.getMessage());
        }
    }

    @Override
    public ProviderFileMetadata getFileMetadata(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GRAPH_BASE + "/me/drive/items/" + providerFileId))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = executeWithRetry(request, connection);
            if (response.statusCode() == 404) {
                throw new ResourceNotFoundException("OneDrive file not found: " + providerFileId);
            }
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Microsoft Graph error: " + response.body());
            }

            JsonNode item = objectMapper.readTree(response.body());
            return mapDriveItemToMetadata(item);
        } catch (ResourceNotFoundException | BadRequestException ex) {
            throw ex;
        } catch (Exception e) {
            throw new BadRequestException("Failed to fetch OneDrive file metadata: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GRAPH_BASE + "/me/drive/items/" + providerFileId + "/content"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Failed to download file from OneDrive. HTTP status: " + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            log.error("Error downloading file {} from OneDrive: {}", providerFileId, e.getMessage());
            throw new BadRequestException("Failed to download file from OneDrive: " + e.getMessage());
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
            throw new BadRequestException("Error reading OneDrive file content: " + e.getMessage());
        }
    }

    @Override
    public boolean moveToTrash(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GRAPH_BASE + "/me/drive/items/" + providerFileId))
                    .header("Authorization", "Bearer " + accessToken)
                    .DELETE()
                    .build();

            HttpResponse<String> response = executeWithRetry(request, connection);
            // In OneDrive Graph API, DELETE /me/drive/items/{id} sends item to Recycle Bin and returns 204 No Content
            if (response.statusCode() == 204 || response.statusCode() == 200) {
                log.info("Successfully moved OneDrive file {} to Recycle Bin", providerFileId);
                return true;
            }
            throw new BadRequestException("OneDrive trash failed: " + response.body());
        } catch (Exception e) {
            log.error("Error moving OneDrive file {} to recycle bin: {}", providerFileId, e.getMessage());
            throw new BadRequestException("Failed to move OneDrive file to recycle bin: " + e.getMessage());
        }
    }

    @Override
    public boolean restoreFile(String connectionId, String providerFileId) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GRAPH_BASE + "/me/drive/items/" + providerFileId + "/restore"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = executeWithRetry(request, connection);
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                log.info("Successfully restored OneDrive file {}", providerFileId);
                return true;
            }
            throw new BadRequestException("OneDrive restore failed: " + response.body());
        } catch (Exception e) {
            log.error("Error restoring OneDrive file {}: {}", providerFileId, e.getMessage());
            throw new BadRequestException("Failed to restore OneDrive file: " + e.getMessage());
        }
    }

    @Override
    public StorageUsage getStorageUsage(String connectionId) {
        StorageConnection connection = getConnection(connectionId);
        String accessToken = getValidAccessToken(connection);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GRAPH_BASE + "/me/drive"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = executeWithRetry(request, connection);
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode quota = root.path("quota");
                long total = quota.path("total").asLong(1099511627776L); // default 1TB
                long used = quota.path("used").asLong(0L);
                long remaining = quota.path("remaining").asLong(total - used);
                long deleted = quota.path("deleted").asLong(0L);

                return new StorageUsage(total, used, remaining, deleted);
            }
        } catch (Exception e) {
            log.warn("Could not retrieve OneDrive storage quota: {}", e.getMessage());
        }
        return new StorageUsage(1099511627776L, connection.getUsedBytes(), 1099511627776L - connection.getUsedBytes());
    }

    private ProviderFileMetadata mapDriveItemToMetadata(JsonNode item) {
        String id = item.path("id").asText();
        String name = item.path("name").asText("untitled");
        long size = item.path("size").asLong(0L);

        JsonNode fileNode = item.path("file");
        String mimeType = fileNode.path("mimeType").asText("application/octet-stream");

        String extension = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            extension = name.substring(dot).toLowerCase();
        }

        Instant created = null;
        if (item.has("createdDateTime")) {
            try { created = Instant.parse(item.path("createdDateTime").asText()); } catch (Exception ignored) {}
        }

        Instant modified = null;
        if (item.has("lastModifiedDateTime")) {
            try { modified = Instant.parse(item.path("lastModifiedDateTime").asText()); } catch (Exception ignored) {}
        }

        Instant accessed = null;
        boolean accessedReliable = false;
        JsonNode fsInfo = item.path("fileSystemInfo");
        if (fsInfo.has("lastAccessedDateTime")) {
            try {
                accessed = Instant.parse(fsInfo.path("lastAccessedDateTime").asText());
                accessedReliable = true;
            } catch (Exception ignored) {}
        }

        String sha256 = null;
        JsonNode hashes = fileNode.path("hashes");
        if (hashes.has("sha256Hash")) {
            sha256 = hashes.path("sha256Hash").asText();
        } else if (hashes.has("quickXorHash")) {
            sha256 = hashes.path("quickXorHash").asText();
        }

        ProviderFileMetadata meta = new ProviderFileMetadata();
        meta.setProviderFileId(id);
        meta.setName(name);
        meta.setExtension(extension);
        meta.setMimeType(mimeType);
        meta.setSizeBytes(size);
        meta.setStorageLocation("OneDrive:/" + name);
        meta.setStorageType("onedrive");
        meta.setCreatedTime(created);
        meta.setModifiedTime(modified);
        meta.setLastAccessedTime(accessed);
        meta.setLastAccessedReliable(accessedReliable);
        meta.setSha256Hash(sha256);
        meta.setWebViewLink(item.path("webUrl").asText(null));
        meta.setDirectory(false);
        meta.setTrashed(false);
        return meta;
    }

    private HttpResponse<String> executeWithRetry(HttpRequest request, StorageConnection connection) throws Exception {
        int maxRetries = 2;
        int attempt = 0;

        while (attempt <= maxRetries) {
            attempt++;
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401 && attempt <= maxRetries) {
                log.warn("OneDrive access token expired (401), refreshing token...");
                String newAccessToken = oneDriveOAuthService.refreshAccessToken(connection);
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
                log.warn("OneDrive rate limit reached (429), backing off for {} seconds...", backoffSec);
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
            throw new BadRequestException("No access token found for OneDrive connection");
        }
        if (connection.getTokenExpiresAt() != null && connection.getTokenExpiresAt().isBefore(Instant.now().plusSeconds(60))) {
            return oneDriveOAuthService.refreshAccessToken(connection);
        }
        return encryptionUtils.decrypt(connection.getEncryptedAccessToken());
    }
}
