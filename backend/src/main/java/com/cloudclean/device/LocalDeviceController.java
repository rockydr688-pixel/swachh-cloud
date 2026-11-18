package com.cloudclean.device;

import com.cloudclean.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Local Devices", description = "Management and registration of local storage workstations and scanning agents")
@SecurityRequirement(name = "Bearer Authentication")
public class LocalDeviceController {

    private final LocalDeviceService localDeviceService;

    public LocalDeviceController(LocalDeviceService localDeviceService) {
        this.localDeviceService = localDeviceService;
    }

    @GetMapping
    @Operation(summary = "List all registered local devices for the authenticated user")
    public ResponseEntity<ApiResponse<List<LocalDevice>>> getDevices(
            @AuthenticationPrincipal(expression = "id") String userId) {
        List<LocalDevice> devices = localDeviceService.getUserDevices(userId);
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    @PostMapping("/register")
    @Operation(summary = "Register or synchronize local agent device metadata")
    public ResponseEntity<ApiResponse<LocalDevice>> registerDevice(
            @AuthenticationPrincipal(expression = "id") String userId,
            @Valid @RequestBody LocalDevice device) {
        LocalDevice saved = localDeviceService.registerOrUpdateDevice(userId, device);
        return ResponseEntity.ok(ApiResponse.success("Local device registered successfully", saved));
    }

    @PostMapping("/{id}/heartbeat")
    @Operation(summary = "Signal live heartbeat from local agent daemon")
    public ResponseEntity<ApiResponse<LocalDevice>> heartbeat(
            @AuthenticationPrincipal(expression = "id") String userId,
            @PathVariable String id) {
        LocalDevice updated = localDeviceService.heartbeat(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Heartbeat acknowledged", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Unlink and remove a local device registration")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(
            @AuthenticationPrincipal(expression = "id") String userId,
            @PathVariable String id) {
        localDeviceService.removeDevice(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Device removed successfully", null));
    }
}
