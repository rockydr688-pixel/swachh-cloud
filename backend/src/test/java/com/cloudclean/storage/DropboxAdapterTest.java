package com.cloudclean.storage;

import com.cloudclean.repository.StorageConnectionRepository;
import com.cloudclean.storage.dropbox.DropboxAdapter;
import com.cloudclean.storage.dropbox.DropboxOAuthService;
import com.cloudclean.storage.provider.StorageUsage;
import com.cloudclean.util.EncryptionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DropboxAdapterTest {

    private StorageConnectionRepository connectionRepository;
    private EncryptionUtils encryptionUtils;
    private DropboxOAuthService dropboxOAuthService;
    private ObjectMapper objectMapper;
    private DropboxAdapter adapter;

    @BeforeEach
    void setUp() {
        connectionRepository = Mockito.mock(StorageConnectionRepository.class);
        encryptionUtils = new EncryptionUtils("cloudclean_oauth_token_encryption_key_32chars!");
        dropboxOAuthService = Mockito.mock(DropboxOAuthService.class);
        objectMapper = new ObjectMapper();
        adapter = new DropboxAdapter(connectionRepository, encryptionUtils, dropboxOAuthService, objectMapper);
    }

    @Test
    void testProviderType_ReturnsDropbox() {
        assertEquals("dropbox", adapter.getProviderType());
    }

    @Test
    void testStorageUsageFallback_WhenNetworkOffline() {
        StorageConnection connection = new StorageConnection("user-1", "Dropbox Personal", "dropbox");
        connection.setId("dbx-1");
        connection.setUsedBytes(25000000L);
        connection.setEncryptedAccessToken(encryptionUtils.encrypt("test-dbx-token"));

        when(connectionRepository.findById("dbx-1")).thenReturn(Optional.of(connection));

        StorageUsage usage = adapter.getStorageUsage("dbx-1");
        assertNotNull(usage);
        assertTrue(usage.getTotalCapacityBytes() >= 2147483648L);
        assertEquals(25000000L, usage.getUsedBytes());
    }

    @Test
    void testTokenAutoRefresh_WhenExpired() {
        StorageConnection connection = new StorageConnection("user-1", "Dropbox Pro", "dropbox");
        connection.setId("dbx-2");
        connection.setEncryptedAccessToken(encryptionUtils.encrypt("expired-token"));
        connection.setEncryptedRefreshToken(encryptionUtils.encrypt("refresh-token"));
        connection.setTokenExpiresAt(Instant.now().minusSeconds(300)); // Expired

        when(connectionRepository.findById("dbx-2")).thenReturn(Optional.of(connection));
        when(dropboxOAuthService.refreshAccessToken(connection)).thenReturn("fresh-dbx-token");

        StorageUsage usage = adapter.getStorageUsage("dbx-2");
        assertNotNull(usage);

        verify(dropboxOAuthService, atLeastOnce()).refreshAccessToken(connection);
    }
}
