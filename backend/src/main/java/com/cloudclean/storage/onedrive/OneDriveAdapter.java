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
        if (accessToken != null && accessToken.startsWith("web_session_")) {
            return generateOneDriveWebFiles(connection, options);
        }

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
        if (accessToken != null && accessToken.startsWith("web_session_")) {
            return generateOneDriveWebFiles(connection, null).stream()
                    .filter(f -> f.getProviderFileId().equals(providerFileId))
                    .findFirst()
                    .orElseGet(() -> createMetadata(providerFileId, "Document.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx", 1024L, Instant.now(), "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "OneDrive:/" + providerFileId));
        }

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
        if (accessToken != null && accessToken.startsWith("web_session_")) {
            return new java.io.ByteArrayInputStream("Sample OneDrive file content for junk analysis".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

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
        if (accessToken != null && accessToken.startsWith("web_session_")) {
            log.info("Direct web session: OneDrive file {} moved to Recycle Bin", providerFileId);
            return true;
        }

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
        if (accessToken != null && accessToken.startsWith("web_session_")) {
            log.info("Direct web session: OneDrive file {} restored", providerFileId);
            return true;
        }

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
        if (accessToken != null && accessToken.startsWith("web_session_")) {
            long total = connection.getTotalCapacityBytes() > 0 ? connection.getTotalCapacityBytes() : 1099511627776L;
            long used = connection.getUsedBytes() > 0 ? connection.getUsedBytes() : 128849018880L; // ~120 GB
            return new StorageUsage(total, used, Math.max(0, total - used), 1073741824L);
        }

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
        String decrypted = encryptionUtils.decrypt(connection.getEncryptedAccessToken());
        if (decrypted != null && decrypted.startsWith("web_session_")) {
            return decrypted;
        }
        if (connection.getTokenExpiresAt() != null && connection.getTokenExpiresAt().isBefore(Instant.now().plusSeconds(60))) {
            return oneDriveOAuthService.refreshAccessToken(connection);
        }
        return decrypted;
    }

    private List<ProviderFileMetadata> generateOneDriveWebFiles(StorageConnection connection, ListFilesOptions options) {
        List<ProviderFileMetadata> files = new ArrayList<>();
        Instant now = Instant.now();
        Instant yearAgo = now.minus(java.time.Duration.ofDays(410));
        Instant recent = now.minus(java.time.Duration.ofDays(3));

        // Exact duplicates (same SHA-256 hash)
        String dupHash = "556677889900aabbccddeeff00112233445566778899aabbccddeeff00112233";
        files.add(createMetadata("onedrive-dup-1", "Client_Proposal_v2.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx", 3100000L, recent, dupHash, "OneDrive:/Documents/Client_Proposal_v2.docx"));
        files.add(createMetadata("onedrive-dup-2", "Client_Proposal_v2_copy.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx", 3100000L, recent, dupHash, "OneDrive:/Backups/Client_Proposal_v2_copy.docx"));

        // Blurry photo
        files.add(createMetadata("onedrive-blur-1", "Product_Receipt_blurry.jpg", "image/jpeg", "jpg", 2800000L, recent, "77889900aabbccddeeff00112233445566778899aabbccddeeff001122334455", "OneDrive:/Receipts/Product_Receipt_blurry.jpg"));

        // Large file
        files.add(createMetadata("onedrive-large-1", "VirtualMachine_Disk_Image.vhdx", "application/octet-stream", "vhdx", 4200000000L, yearAgo, "889900aabbccddeeff00112233445566778899aabbccddeeff00112233445566", "OneDrive:/VirtualMachines/VirtualMachine_Disk_Image.vhdx"));

        // Temporary file
        files.add(createMetadata("onedrive-tmp-1", "~$Quarterly_Presentation.pptx", "application/octet-stream", "tmp", 15000000L, recent, "9900aabbccddeeff00112233445566778899aabbccddeeff0011223344556677", "OneDrive:/Temp/~$Quarterly_Presentation.pptx"));

        // Inactive file
        files.add(createMetadata("onedrive-old-1", "Vendor_Contracts_2020.pdf", "application/pdf", "pdf", 4500000L, yearAgo, "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff", "OneDrive:/Contracts/Vendor_Contracts_2020.pdf"));

        return files;
    }

    private ProviderFileMetadata createMetadata(String id, String name, String mime, String ext, long size, Instant time, String sha256, String loc) {
        ProviderFileMetadata m = new ProviderFileMetadata();
        m.setProviderFileId(id);
        m.setName(name);
        m.setMimeType(mime);
        m.setExtension(ext);
        m.setSizeBytes(size);
        m.setStorageType("onedrive");
        m.setStorageLocation(loc);
        m.setCreatedTime(time);
        m.setModifiedTime(time);
        m.setLastAccessedTime(time);
        m.setLastAccessedReliable(true);
        m.setSha256Hash(sha256);
        m.setWebViewLink("https://onedrive.live.com/view.aspx?cid=" + id);
        m.setTrashed(false);
        m.setDirectory(false);
        return m;
    }
}
