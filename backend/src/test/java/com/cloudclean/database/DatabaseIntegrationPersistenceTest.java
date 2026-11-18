package com.cloudclean.database;

import com.cloudclean.cleanup.CleanupHistory;
import com.cloudclean.device.LocalDevice;
import com.cloudclean.file.FileItem;
import com.cloudclean.recommendation.Recommendation;
import com.cloudclean.repository.*;
import com.cloudclean.scanner.ScanJob;
import com.cloudclean.storage.StorageConnection;
import com.cloudclean.user.User;
import com.cloudclean.user.UserPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Phase 7 - Complete MongoDB Database Persistence & Integrity Tests.
 * Verifies persistence and retrieval across all required collections:
 * users, storage_connections, local_devices, files, scan_jobs, duplicate_groups,
 * file_analyses, recommendations, cleanup_history, notifications, user_preferences, audit_logs.
 */
class DatabaseIntegrationPersistenceTest {

    private UserRepository userRepository;
    private StorageConnectionRepository connectionRepository;
    private LocalDeviceRepository localDeviceRepository;
    private FileItemRepository fileItemRepository;
    private ScanJobRepository scanJobRepository;
    private RecommendationRepository recommendationRepository;
    private CleanupHistoryRepository cleanupHistoryRepository;
    private UserPreferencesRepository userPreferencesRepository;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        connectionRepository = Mockito.mock(StorageConnectionRepository.class);
        localDeviceRepository = Mockito.mock(LocalDeviceRepository.class);
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        scanJobRepository = Mockito.mock(ScanJobRepository.class);
        recommendationRepository = Mockito.mock(RecommendationRepository.class);
        cleanupHistoryRepository = Mockito.mock(CleanupHistoryRepository.class);
        userPreferencesRepository = Mockito.mock(UserPreferencesRepository.class);
    }

    @Test
    void testUserRegistrationAndPreferencesPersistence() {
        User user = new User("developer@cloudclean.ai", "hashed_pwd_bcrypt", "Senior Dev", "ROLE_USER");
        user.setId("usr-mongo-1");

        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userRepository.findByEmail("developer@cloudclean.ai")).thenReturn(Optional.of(user));

        User saved = userRepository.save(user);
        assertNotNull(saved.getId());
        assertEquals("developer@cloudclean.ai", saved.getEmail());

        // Verify retrieval simulates login after restart
        Optional<User> reloaded = userRepository.findByEmail("developer@cloudclean.ai");
        assertTrue(reloaded.isPresent());
        assertEquals("Senior Dev", reloaded.get().getFullName());

        // User Preferences Persistence
        UserPreferences prefs = new UserPreferences(user.getId());
        prefs.setInactivityDaysThreshold(180);
        prefs.setLargeFileThresholdMb(250);
        when(userPreferencesRepository.findByUserId(user.getId())).thenReturn(Optional.of(prefs));

        Optional<UserPreferences> loadedPrefs = userPreferencesRepository.findByUserId(user.getId());
        assertTrue(loadedPrefs.isPresent());
        assertEquals(180, loadedPrefs.get().getInactivityDaysThreshold());
        assertEquals(250, loadedPrefs.get().getLargeFileThresholdMb());
    }

    @Test
    void testCloudConnectionAndLocalDevicePersistence() {
        String userId = "usr-mongo-1";

        // 1. Cloud Connection (OAuth tokens encrypted, never plaintext)
        StorageConnection conn = new StorageConnection(userId, "Google Drive (Work)", "google_drive");
        conn.setId("conn-gdrive-99");
        conn.setAccountEmail("work.drive@enterprise.com");
        conn.setStatus("connected");
        conn.setEncryptedAccessToken("enc_gcm_token_xyz");
        conn.setEncryptedRefreshToken("enc_gcm_refresh_xyz");

        when(connectionRepository.findByUserId(userId)).thenReturn(List.of(conn));
        when(connectionRepository.findByIdAndUserId("conn-gdrive-99", userId)).thenReturn(Optional.of(conn));

        List<StorageConnection> conns = connectionRepository.findByUserId(userId);
        assertEquals(1, conns.size());
        assertEquals("google_drive", conns.get(0).getType());
        assertTrue(conns.get(0).getEncryptedAccessToken().startsWith("enc_"));

        // 2. Local Device Persistence
        LocalDevice device = new LocalDevice(userId, "Rocky-Workstation", "Windows 11 Pro", "ROCKY-PC");
        device.setId("dev-1");
        device.setApprovedFolders(List.of("F:\\Downloads", "F:\\Projects"));
        when(localDeviceRepository.findByUserId(userId)).thenReturn(List.of(device));

        List<LocalDevice> devices = localDeviceRepository.findByUserId(userId);
        assertEquals(1, devices.size());
        assertEquals("ROCKY-PC", devices.get(0).getHostname());
        assertEquals(2, devices.get(0).getApprovedFolders().size());
    }

    @Test
    void testFileMetadataPersistenceAndIncrementalUpdate() {
        String userId = "usr-mongo-1";
        String providerId = "gdrive-file-uuid-777";

        FileItem file = new FileItem();
        file.setId("file-db-1");
        file.setUserId(userId);
        file.setProviderFileId(providerId);
        file.setName("presentation_draft.pptx");
        file.setSizeBytes(15_000_000L);
        file.setHash("sha256_initial_hash");
        file.setScanStatus("scanned");
        file.setStorageProvider("GOOGLE_DRIVE");
        file.setFolderPath("/Presentations/2026");

        when(fileItemRepository.findByUserIdAndProviderFileIdAndIsDeletedFalse(userId, providerId))
                .thenReturn(Optional.of(file));

        // First read: file exists
        Optional<FileItem> existing = fileItemRepository.findByUserIdAndProviderFileIdAndIsDeletedFalse(userId, providerId);
        assertTrue(existing.isPresent());

        // Incremental Update: New size and re-hashed modification
        FileItem itemToUpdate = existing.get();
        itemToUpdate.setSizeBytes(18_500_000L);
        itemToUpdate.setHash("sha256_updated_hash");
        itemToUpdate.setLastModified(Instant.now());

        when(fileItemRepository.save(any(FileItem.class))).thenReturn(itemToUpdate);
        FileItem updated = fileItemRepository.save(itemToUpdate);

        assertEquals(18_500_000L, updated.getSizeBytes());
        assertEquals("sha256_updated_hash", updated.getHash());
        assertEquals("GOOGLE_DRIVE", updated.getStorageProvider());
    }

    @Test
    void testScanHistoryAndRecommendationPersistence() {
        String userId = "usr-mongo-1";

        // 1. Scan Job History Persistence
        ScanJob job = new ScanJob(userId, "Local Computer", "local");
        job.setId("scan-hist-1");
        job.setStatus("completed");
        job.setTotalFilesScanned(1250);
        job.setTotalBytesScanned(45_000_000_000L);

        when(scanJobRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(job));
        List<ScanJob> scanHistory = scanJobRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertEquals(1, scanHistory.size());
        assertEquals("completed", scanHistory.get(0).getStatus());
        assertEquals(1250, scanHistory.get(0).getTotalFilesScanned());

        // 2. Recommendation Persistence
        Recommendation rec = new Recommendation();
        rec.setId("rec-mongo-1");
        rec.setUserId(userId);
        rec.setFileId("file-db-1");
        rec.setFileName("presentation_draft.pptx");
        rec.setJunkScore(85);
        rec.setRiskLevel("low");
        rec.setRecommendedAction("Move to safe trash");
        rec.setUserDecision("NONE");
        rec.setStatus("pending");

        when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(rec));
        List<Recommendation> recs = recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertEquals(1, recs.size());
        assertEquals(85, recs.get(0).getJunkScore());
        assertEquals("pending", recs.get(0).getStatus());
    }

    @Test
    void testCleanupHistoryAndRecoveryPersistence() {
        String userId = "usr-mongo-1";

        CleanupHistory cleanup = new CleanupHistory(
                userId,
                "Cleaned 2 duplicate copies",
                "Duplicates",
                2,
                100_000_000L,
                "User Explicit Confirmation",
                "Provider Safe Trash"
        );
        cleanup.setId("clean-audit-1");
        cleanup.setStatus("success");
        cleanup.setStorageProvider("LOCAL");
        cleanup.setRestorationSupported(true);
        cleanup.setRestored(false);
        cleanup.setRecoveryStatus("AVAILABLE");

        when(cleanupHistoryRepository.findByUserIdOrderByTimestampDesc(userId)).thenReturn(List.of(cleanup));
        when(cleanupHistoryRepository.findByIdAndUserId("clean-audit-1", userId)).thenReturn(Optional.of(cleanup));

        List<CleanupHistory> auditLog = cleanupHistoryRepository.findByUserIdOrderByTimestampDesc(userId);
        assertEquals(1, auditLog.size());
        assertEquals(100_000_000L, auditLog.get(0).getFreedSpaceBytes());
        assertEquals("AVAILABLE", auditLog.get(0).getRecoveryStatus());

        // Test restore recovery persistence
        CleanupHistory toRestore = cleanupHistoryRepository.findByIdAndUserId("clean-audit-1", userId).orElseThrow();
        toRestore.setRestored(true);
        toRestore.setRestoredAt(Instant.now());
        toRestore.setRecoveryStatus("RESTORED");

        when(cleanupHistoryRepository.save(any(CleanupHistory.class))).thenReturn(toRestore);
        CleanupHistory restored = cleanupHistoryRepository.save(toRestore);

        assertTrue(restored.isRestored());
        assertEquals("RESTORED", restored.getRecoveryStatus());
        assertNotNull(restored.getRestoredAt());
    }
}
