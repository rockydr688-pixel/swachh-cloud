package com.cloudclean.storage;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.dto.GoogleConnectRequest;
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

@RestController
@RequestMapping({"/api/storage", "/api/v1/storage"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Storage Connections", description = "Endpoints to register local directories and connect Google Drive storage")
public class StorageController {

    private final StorageConnectionService storageConnectionService;

    public StorageController(StorageConnectionService storageConnectionService) {
        this.storageConnectionService = storageConnectionService;
    }

    @GetMapping
    @Operation(summary = "List all storage connections", description = "Returns status of local computer daemon and Google Drive adapters for current user")
    public ResponseEntity<ApiResponse<List<StorageConnectionResponse>>> getStorageConnections() {
        String userId = SecurityUtils.getCurrentUserId();
        List<StorageConnectionResponse> connections = storageConnectionService.getUserConnections(userId);
        return ResponseEntity.ok(ApiResponse.success(connections));
    }

    @PostMapping("/local/register")
    @Operation(summary = "Register local directory", description = "Registers folders monitored by the local agent daemon")
    public ResponseEntity<ApiResponse<StorageConnectionResponse>> registerLocalFolder(@Valid @RequestBody StorageConnectionRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        StorageConnectionResponse response = storageConnectionService.registerLocalConnection(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Local storage folders registered", response));
    }

    @PostMapping("/google/connect")
    @Operation(summary = "Connect Google Drive", description = "Encrypts OAuth authorization code and creates Google Drive connection")
    public ResponseEntity<ApiResponse<StorageConnectionResponse>> connectGoogleDrive(@Valid @RequestBody GoogleConnectRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        StorageConnectionResponse response = storageConnectionService.connectGoogleDrive(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Google Drive OAuth connection established", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Disconnect storage", description = "Disconnects storage adapter and invalidates stored tokens")
    public ResponseEntity<ApiResponse<Void>> disconnectStorage(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        storageConnectionService.disconnectConnection(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Storage connection disconnected successfully", null));
    }
}
