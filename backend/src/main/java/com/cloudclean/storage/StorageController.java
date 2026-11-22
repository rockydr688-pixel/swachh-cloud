package com.cloudclean.storage;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.dto.CloudConnectRequest;
import com.cloudclean.dto.GoogleConnectRequest;
import com.cloudclean.dto.ProviderStatusResponse;
import com.cloudclean.dto.StorageConnectionRequest;
import com.cloudclean.dto.StorageConnectionResponse;
import com.cloudclean.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/storage", "/api/v1/storage"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Storage Connections", description = "Endpoints to register local directories and connect Google Drive, OneDrive, and Dropbox storage")
public class StorageController {

    private final StorageConnectionService storageConnectionService;

    public StorageController(StorageConnectionService storageConnectionService) {
        this.storageConnectionService = storageConnectionService;
    }

    @GetMapping
    @Operation(summary = "List all storage connections", description = "Returns live status and quota of local computer daemon, Google Drive, OneDrive, and Dropbox adapters for current user")
    public ResponseEntity<ApiResponse<List<StorageConnectionResponse>>> getStorageConnections() {
        String userId = SecurityUtils.getCurrentUserId();
        List<StorageConnectionResponse> connections = storageConnectionService.getUserConnections(userId);
        return ResponseEntity.ok(ApiResponse.success(connections));
    }

    @GetMapping("/providers/status")
    @Operation(summary = "Get provider integration status", description = "Returns configuration status, OAuth URL, and setup instructions for each supported provider")
    public ResponseEntity<ApiResponse<List<ProviderStatusResponse>>> getProviderStatuses(
            @RequestParam(required = false) String redirectUri) {
        List<ProviderStatusResponse> statuses = storageConnectionService.getProviderStatuses(redirectUri);
        return ResponseEntity.ok(ApiResponse.success(statuses));
    }

    @GetMapping("/google/auth-url")
    @Operation(summary = "Get Google Drive OAuth authorization URL", description = "Generates Google OAuth 2.0 authorization URL with minimum drive scopes")
    public ResponseEntity<ApiResponse<Map<String, String>>> getGoogleAuthUrl(
            @RequestParam(required = false) String redirectUri) {
        String userId = SecurityUtils.getCurrentUserId();
        String url = storageConnectionService.getGoogleAuthUrl(userId, redirectUri);
        return ResponseEntity.ok(ApiResponse.success(Map.of("authorizationUrl", url != null ? url : "")));
    }

    @PostMapping("/local/register")
    @Operation(summary = "Register local directory", description = "Registers folders monitored by the local agent daemon")
    public ResponseEntity<ApiResponse<StorageConnectionResponse>> registerLocalFolder(@Valid @RequestBody StorageConnectionRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        StorageConnectionResponse response = storageConnectionService.registerLocalConnection(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Local storage folders registered", response));
    }

    @PostMapping("/local/validate-path")
    @Operation(summary = "Validate local folder path", description = "Verifies path existence, directory check, and readability on the host")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateLocalPath(@RequestBody Map<String, String> request) {
        String userId = SecurityUtils.getCurrentUserId();
        String path = request.get("path");
        Map<String, Object> result = storageConnectionService.validateLocalPath(userId, path);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/google/connect")
    @Operation(summary = "Connect Google Drive", description = "Exchanges OAuth authorization code and creates Google Drive connection")
    public ResponseEntity<ApiResponse<StorageConnectionResponse>> connectGoogleDrive(@Valid @RequestBody GoogleConnectRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        StorageConnectionResponse response = storageConnectionService.connectGoogleDrive(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Google Drive OAuth connection established", response));
    }

    @PostMapping("/cloud/connect")
    @Operation(summary = "Connect cloud account", description = "Connects Google Drive, OneDrive, or Dropbox account via OAuth 2.0 authorization code")
    public ResponseEntity<ApiResponse<StorageConnectionResponse>> connectCloudAccount(@Valid @RequestBody CloudConnectRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        StorageConnectionResponse response = storageConnectionService.connectCloudAccount(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cloud account connected successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Disconnect storage", description = "Disconnects storage adapter and invalidates stored tokens")
    public ResponseEntity<ApiResponse<Void>> disconnectStorage(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        storageConnectionService.disconnectConnection(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Storage connection disconnected successfully", null));
    }
}
