package com.cloudclean.storage;

import com.cloudclean.repository.StorageConnectionRepository;
import com.cloudclean.storage.onedrive.OneDriveAdapter;
import com.cloudclean.storage.onedrive.OneDriveOAuthService;
import com.cloudclean.storage.provider.StorageUsage;
import com.cloudclean.util.EncryptionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OneDriveAdapterTest {

    private StorageConnectionRepository connectionRepository;
    private EncryptionUtils encryptionUtils;
    private OneDriveOAuthService oneDriveOAuthService;
    private ObjectMapper objectMapper;
    private OneDriveAdapter adapter;

    @BeforeEach
    void setUp() {
        connectionRepository = Mockito.mock(StorageConnectionRepository.class);
        encryptionUtils = new EncryptionUtils("cloudclean_oauth_token_encryption_key_32chars!");
        oneDriveOAuthService = Mockito.mock(OneDriveOAuthService.class);
        objectMapper = new ObjectMapper();
        adapter = new OneDriveAdapter(connectionRepository, encryptionUtils, oneDriveOAuthService, objectMapper);
    }

    @Test
    void testProviderType_ReturnsOnedrive() {
        assertEquals("onedrive", adapter.getProviderType());
    }

    @Test
    void testStorageUsageFallback_WhenNetworkOffline() {
        StorageConnection connection = new StorageConnection("user-1", "OneDrive Personal", "onedrive");
        connection.setId("onedrive-1");
        connection.setUsedBytes(50000000L);
        connection.setEncryptedAccessToken(encryptionUtils.encrypt("test-token"));

        when(connectionRepository.findById("onedrive-1")).thenReturn(Optional.of(connection));

        StorageUsage usage = adapter.getStorageUsage("onedrive-1");
        assertNotNull(usage);
        assertTrue(usage.getTotalCapacityBytes() >= 1099511627776L);
        assertEquals(50000000L, usage.getUsedBytes());
    }

    @Test
    void testTokenAutoRefresh_WhenExpired() {
        StorageConnection connection = new StorageConnection("user-1", "OneDrive Business", "onedrive");
        connection.setId("onedrive-2");
        connection.setEncryptedAccessToken(encryptionUtils.encrypt("expired-token"));
        connection.setEncryptedRefreshToken(encryptionUtils.encrypt("refresh-token"));
        connection.setTokenExpiresAt(Instant.now().minusSeconds(120)); // Expired

        when(connectionRepository.findById("onedrive-2")).thenReturn(Optional.of(connection));
        when(oneDriveOAuthService.refreshAccessToken(connection)).thenReturn("fresh-new-token");

        // Attempting to query storage usage should trigger token refresh check
        StorageUsage usage = adapter.getStorageUsage("onedrive-2");
        assertNotNull(usage);

        verify(oneDriveOAuthService, atLeastOnce()).refreshAccessToken(connection);
    }
}
