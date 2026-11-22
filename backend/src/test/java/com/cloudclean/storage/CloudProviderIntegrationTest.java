package com.cloudclean.storage;

import com.cloudclean.cleanup.CleanupService;
import com.cloudclean.dto.CleanupImpactResponse;
import com.cloudclean.dto.CleanupRequest;
import com.cloudclean.dto.ProviderStatusResponse;
import com.cloudclean.file.FileItem;
import com.cloudclean.repository.CleanupHistoryRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.RecommendationRepository;
import com.cloudclean.repository.StorageConnectionRepository;
import com.cloudclean.storage.agent.LocalAgentScanner;
import com.cloudclean.storage.dropbox.DropboxOAuthService;
import com.cloudclean.storage.google.GoogleOAuthService;
import com.cloudclean.storage.onedrive.OneDriveOAuthService;
import com.cloudclean.storage.provider.StorageProviderRegistry;
import com.cloudclean.util.EncryptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloudProviderIntegrationTest {

    private StorageConnectionRepository connectionRepository;
    private EncryptionUtils encryptionUtils;
    private GoogleOAuthService googleOAuthService;
    private OneDriveOAuthService oneDriveOAuthService;
    private DropboxOAuthService dropboxOAuthService;
    private StorageProviderRegistry storageProviderRegistry;
    private LocalAgentScanner localAgentScanner;
    private StorageConnectionService connectionService;

    @BeforeEach
    void setUp() {
        connectionRepository = Mockito.mock(StorageConnectionRepository.class);
        encryptionUtils = new EncryptionUtils("cloudclean_oauth_token_encryption_key_32chars!");
        googleOAuthService = Mockito.mock(GoogleOAuthService.class);
        oneDriveOAuthService = Mockito.mock(OneDriveOAuthService.class);
        dropboxOAuthService = Mockito.mock(DropboxOAuthService.class);
        storageProviderRegistry = Mockito.mock(StorageProviderRegistry.class);
        localAgentScanner = Mockito.mock(LocalAgentScanner.class);

        connectionService = new StorageConnectionService(
                connectionRepository,
                encryptionUtils,
                googleOAuthService,
                oneDriveOAuthService,
                dropboxOAuthService,
                storageProviderRegistry,
                localAgentScanner
        );
    }

    @Test
    void testProviderStatuses_ReturnsAllFourSupportedProviders() {
        when(googleOAuthService.isConfigured()).thenReturn(true);
        when(googleOAuthService.buildAuthorizationUrl(any(), any())).thenReturn("https://accounts.google.com/o/oauth2/v2/auth?scope=drive");
        when(oneDriveOAuthService.isConfigured()).thenReturn(false);
        when(dropboxOAuthService.isConfigured()).thenReturn(false);

        List<ProviderStatusResponse> statuses = connectionService.getProviderStatuses("http://localhost:3000/oauth/callback");

        assertEquals(4, statuses.size());

        // Google Drive configured
        ProviderStatusResponse gdrive = statuses.stream().filter(s -> "google_drive".equals(s.getProviderKey())).findFirst().orElseThrow();
        assertTrue(gdrive.isConfigured());
        assertNotNull(gdrive.getAuthorizationUrl());
        assertNull(gdrive.getSetupInstructions());

        // OneDrive awaiting configuration
        ProviderStatusResponse onedrive = statuses.stream().filter(s -> "onedrive".equals(s.getProviderKey())).findFirst().orElseThrow();
        assertFalse(onedrive.isConfigured());
        assertNotNull(onedrive.getSetupInstructions());
        assertTrue(onedrive.getSetupInstructions().contains("ONEDRIVE_CLIENT_ID"));

        // Dropbox awaiting configuration
        ProviderStatusResponse dropbox = statuses.stream().filter(s -> "dropbox".equals(s.getProviderKey())).findFirst().orElseThrow();
        assertFalse(dropbox.isConfigured());
        assertNotNull(dropbox.getSetupInstructions());
        assertTrue(dropbox.getSetupInstructions().contains("DROPBOX_CLIENT_ID"));

        // Local storage always configured
        ProviderStatusResponse local = statuses.stream().filter(s -> "local".equals(s.getProviderKey())).findFirst().orElseThrow();
        assertTrue(local.isConfigured());
    }

    @Test
    void testMultipleAccountsPerProvider_DistinctConnectionRecords() {
        String userId = "user-multi-account";

        StorageConnection conn1 = new StorageConnection(userId, "Google Drive (work@corp.com)", "google_drive");
        conn1.setId("gdrive-work-1");
        conn1.setAccountEmail("work@corp.com");
        conn1.setStatus("connected");

        StorageConnection conn2 = new StorageConnection(userId, "Google Drive (personal@gmail.com)", "google_drive");
        conn2.setId("gdrive-personal-2");
        conn2.setAccountEmail("personal@gmail.com");
        conn2.setStatus("connected");

        when(connectionRepository.findByUserId(userId)).thenReturn(List.of(conn1, conn2));

        var userConnections = connectionService.getUserConnections(userId);
        assertEquals(2, userConnections.size());
        assertNotEquals(userConnections.get(0).getId(), userConnections.get(1).getId());
        assertEquals("work@corp.com", userConnections.get(0).getAccountEmail());
        assertEquals("personal@gmail.com", userConnections.get(1).getAccountEmail());
    }

    @Test
    void testCleanupSafetyNet_NeverDeletesAllDuplicateCopies() {
        FileItemRepository fileItemRepository = Mockito.mock(FileItemRepository.class);
        CleanupHistoryRepository cleanupHistoryRepository = Mockito.mock(CleanupHistoryRepository.class);
        RecommendationRepository recommendationRepository = Mockito.mock(RecommendationRepository.class);
        var notifService = Mockito.mock(com.cloudclean.notification.NotificationService.class);
        var auditLogService = Mockito.mock(com.cloudclean.audit.AuditLogService.class);
        var rateLimiterService = Mockito.mock(com.cloudclean.config.RateLimiterService.class);

        CleanupService cleanupService = new CleanupService(
                cleanupHistoryRepository,
                fileItemRepository,
                recommendationRepository,
                storageProviderRegistry,
                notifService,
                auditLogService,
                rateLimiterService
        );

        String userId = "user-dup-test";
        String sameHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        FileItem copy1 = new FileItem();
        copy1.setId("file-1");
        copy1.setUserId(userId);
        copy1.setName("Document.pdf");
        copy1.setHash(sameHash);
        copy1.setSizeBytes(1024L);
        copy1.setRecommendationStatus("Keep Original");

        FileItem copy2 = new FileItem();
        copy2.setId("file-2");
        copy2.setUserId(userId);
        copy2.setName("Document (Copy).pdf");
        copy2.setHash(sameHash);
        copy2.setSizeBytes(1024L);
        copy2.setRecommendationStatus("Safe to Delete");

        when(fileItemRepository.findByIdInAndUserId(List.of("file-1", "file-2"), userId))
                .thenReturn(List.of(copy1, copy2));
        when(fileItemRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(List.of(copy1, copy2));
        when(cleanupHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CleanupRequest request = new CleanupRequest();
        request.setFileIds(List.of("file-1", "file-2"));
        request.setConfirmed(true);

        var result = cleanupService.executeCleanup(userId, request, "Automated Test");

        // The master copy (copy1) should have been preserved by the safety net!
        assertEquals(1, result.getFilesAffected(), "Only 1 copy should be deleted; the master original must be preserved");
        assertTrue(result.getCleanedFileIds().contains("file-2"));
        assertFalse(result.getCleanedFileIds().contains("file-1"), "Original copy must never be purged");
    }
}
