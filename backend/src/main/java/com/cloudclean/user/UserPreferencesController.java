package com.cloudclean.user;

import com.cloudclean.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/preferences")
@Tag(name = "User Preferences", description = "User-specific threshold preferences, scan frequencies, and notifications")
@SecurityRequirement(name = "Bearer Authentication")
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    public UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }

    @GetMapping
    @Operation(summary = "Get preferences and thresholds for the authenticated user")
    public ResponseEntity<ApiResponse<UserPreferences>> getPreferences(
            @AuthenticationPrincipal(expression = "id") String userId) {
        UserPreferences prefs = userPreferencesService.getUserPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(prefs));
    }

    @PutMapping
    @Operation(summary = "Update preferences and thresholds for the authenticated user")
    public ResponseEntity<ApiResponse<UserPreferences>> updatePreferences(
            @AuthenticationPrincipal(expression = "id") String userId,
            @RequestBody UserPreferences update) {
        UserPreferences saved = userPreferencesService.updateUserPreferences(userId, update);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated successfully", saved));
    }
}
