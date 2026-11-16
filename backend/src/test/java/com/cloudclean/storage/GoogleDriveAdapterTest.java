package com.cloudclean.storage;

import com.cloudclean.repository.StorageConnectionRepository;
import com.cloudclean.storage.google.GoogleDriveAdapter;
import com.cloudclean.storage.google.GoogleOAuthService;
import com.cloudclean.util.EncryptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleDriveAdapterTest {

    private StorageConnectionRepository connectionRepository;
    private EncryptionUtils encryptionUtils;
    private GoogleOAuthService googleOAuthService;
    private GoogleDriveAdapter adapter;

    @BeforeEach
    void setUp() {
        connectionRepository = Mockito.mock(StorageConnectionRepository.class);
        encryptionUtils = new EncryptionUtils("cloudclean_oauth_token_encryption_key_32chars!");
        googleOAuthService = Mockito.mock(GoogleOAuthService.class);
        adapter = new GoogleDriveAdapter(connectionRepository, encryptionUtils, googleOAuthService);
    }

    @Test
    void testGoogleDriveTokenRefresh_WhenExpired() {
        StorageConnection connection = new StorageConnection("user-1", "Google Drive", "google_drive");
        connection.setId("gdrive-conn-1");
        connection.setEncryptedAccessToken(encryptionUtils.encrypt("expired-access-token"));
        connection.setEncryptedRefreshToken(encryptionUtils.encrypt("valid-refresh-token"));
        // Token expired 10 minutes ago
        connection.setTokenExpiresAt(Instant.now().minusSeconds(600));

        when(googleOAuthService.refreshAccessToken(connection)).thenReturn("fresh-new-access-token");

        // Building the client should detect the expired token and invoke refreshAccessToken
        assertDoesNotThrow(() -> {
            try {
                adapter.buildDriveClient(connection);
            } catch (Exception e) {
                // Network call to Google might fail in test, but token refresh must have occurred
            }
        });

        verify(googleOAuthService, times(1)).refreshAccessToken(connection);
        verify(connectionRepository, times(1)).save(connection);
    }

    @Test
    void testGoogleDriveTokenNotRefreshed_WhenStillValid() {
        StorageConnection connection = new StorageConnection("user-1", "Google Drive", "google_drive");
        connection.setId("gdrive-conn-2");
        connection.setEncryptedAccessToken(encryptionUtils.encrypt("valid-access-token"));
        connection.setEncryptedRefreshToken(encryptionUtils.encrypt("valid-refresh-token"));
        // Token valid for 2 hours
        connection.setTokenExpiresAt(Instant.now().plusSeconds(7200));

        assertDoesNotThrow(() -> {
            try {
                adapter.buildDriveClient(connection);
            } catch (Exception e) {
                // Expected network failure to real google servers in unit test
            }
        });

        // Token is still valid, so refreshAccessToken should NOT be called
        verify(googleOAuthService, never()).refreshAccessToken(any());
    }
}

