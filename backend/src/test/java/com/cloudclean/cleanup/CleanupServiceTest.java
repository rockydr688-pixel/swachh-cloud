package com.cloudclean.cleanup;

import com.cloudclean.audit.AuditLogService;
import com.cloudclean.config.RateLimiterService;
import com.cloudclean.dto.CleanupHistoryResponse;
import com.cloudclean.dto.CleanupImpactResponse;
import com.cloudclean.dto.CleanupRequest;
import com.cloudclean.exception.BadRequestException;
import com.cloudclean.file.FileItem;
import com.cloudclean.notification.NotificationService;
import com.cloudclean.repository.CleanupHistoryRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.RecommendationRepository;
import com.cloudclean.storage.provider.StorageProvider;
import com.cloudclean.storage.provider.StorageProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CleanupServiceTest {

    private CleanupHistoryRepository cleanupHistoryRepository;
    private FileItemRepository fileItemRepository;
    private RecommendationRepository recommendationRepository;
    private StorageProviderRegistry storageProviderRegistry;
    private NotificationService notificationService;
    private AuditLogService auditLogService;
    private RateLimiterService rateLimiterService;
    private StorageProvider localStorageProvider;
    private CleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupHistoryRepository = Mockito.mock(CleanupHistoryRepository.class);
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        recommendationRepository = Mockito.mock(RecommendationRepository.class);
        storageProviderRegistry = Mockito.mock(StorageProviderRegistry.class);
        notificationService = Mockito.mock(NotificationService.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        rateLimiterService = Mockito.mock(RateLimiterService.class);
        localStorageProvider = Mockito.mock(StorageProvider.class);

        when(storageProviderRegistry.hasProvider("local")).thenReturn(true);
        when(storageProviderRegistry.getProvider("local")).thenReturn(localStorageProvider);

        cleanupService = new CleanupService(
                cleanupHistoryRepository,
                fileItemRepository,
                recommendationRepository,
                storageProviderRegistry,
                notificationService,
                auditLogService,
                rateLimiterService
        );
    }

    @Test
    void testExecuteCleanupWithoutConfirmation_ThrowsBadRequest() {
        CleanupRequest request = new CleanupRequest();
        request.setConfirmed(false);

        assertThrows(BadRequestException.class, () ->
                cleanupService.executeCleanup("user-1", request, "user-email")
        );
    }

    @Test
    void testCalculateCleanupImpact_SkipsProtectedFiles() {
        String userId = "user-1";
        FileItem normalFile = new FileItem();
        normalFile.setId("file-1");
        normalFile.setUserId(userId);
        normalFile.setSizeBytes(10_000_000L);
        normalFile.setProtected(false);

        FileItem protectedFile = new FileItem();
        protectedFile.setId("file-2");
        protectedFile.setUserId(userId);
        protectedFile.setSizeBytes(50_000_000L);
        protectedFile.setProtected(true);

        when(fileItemRepository.findByIdInAndUserId(List.of("file-1", "file-2"), userId))
                .thenReturn(List.of(normalFile, protectedFile));

        CleanupRequest request = new CleanupRequest();
        request.setFileIds(List.of("file-1", "file-2"));

        CleanupImpactResponse impact = cleanupService.calculateCleanupImpact(userId, request);

        assertEquals(1, impact.getTotalFilesSelected());
        assertEquals(10_000_000L, impact.getEstimatedRecoverableBytes());
        assertEquals(1, impact.getProtectedFilesSkipped());
        assertTrue(impact.getWarningMessage().contains("1 item(s)"));
        assertTrue(impact.getWarningMessage().contains("Protected files (1) will be excluded"));
    }

    @Test
    void testExecuteCleanup_CallsStorageProviderAndRecordsHistory() {
        String userId = "user-1";
        FileItem file = new FileItem();
        file.setId("file-10");
        file.setUserId(userId);
        file.setName("duplicate_backup.zip");
        file.setSizeBytes(100_000_000L);
        file.setStorageType("local");
        file.setStorageConnectionId("conn-1");
        file.setProviderFileId("C:/Users/rocky/Downloads/duplicate_backup.zip");
        file.setProtected(false);

        when(fileItemRepository.findByIdInAndUserId(List.of("file-10"), userId)).thenReturn(List.of(file));
        when(localStorageProvider.moveToTrash("conn-1", "C:/Users/rocky/Downloads/duplicate_backup.zip")).thenReturn(true);
        when(cleanupHistoryRepository.save(any(CleanupHistory.class))).thenAnswer(i -> i.getArgument(0));

        CleanupRequest request = new CleanupRequest(List.of("file-10"), true);
        CleanupHistoryResponse response = cleanupService.executeCleanup(userId, request, "user-tester");

        assertTrue(file.isDeleted());
        assertNotNull(file.getDeletedAt());
        assertEquals("success", response.getStatus());
        assertEquals(100_000_000L, response.getFreedSpaceBytes());
        assertEquals(1, response.getFilesAffected());

        verify(localStorageProvider).moveToTrash("conn-1", "C:/Users/rocky/Downloads/duplicate_backup.zip");
        verify(notificationService).createNotification(eq(userId), anyString(), anyString(), eq("cleanup_completed"), anyString());
    }

    @Test
    void testExecuteCleanup_TracksFailedFilesWhenProviderThrows() {
        String userId = "user-1";
        FileItem file = new FileItem();
        file.setId("file-locked");
        file.setUserId(userId);
        file.setName("locked_system_file.dat");
        file.setSizeBytes(20_000_000L);
        file.setStorageType("local");
        file.setStorageConnectionId("conn-1");
        file.setProviderFileId("C:/locked_system_file.dat");
        file.setProtected(false);

        when(fileItemRepository.findByIdInAndUserId(List.of("file-locked"), userId)).thenReturn(List.of(file));
        when(localStorageProvider.moveToTrash("conn-1", "C:/locked_system_file.dat"))
                .thenThrow(new RuntimeException("Access denied by OS file lock"));
        when(cleanupHistoryRepository.save(any(CleanupHistory.class))).thenAnswer(i -> i.getArgument(0));

        CleanupRequest request = new CleanupRequest(List.of("file-locked"), true);
        CleanupHistoryResponse response = cleanupService.executeCleanup(userId, request, "user-tester");

        assertFalse(file.isDeleted());
        assertEquals("failed", response.getStatus());
        assertEquals(0, response.getFilesAffected());
        assertEquals(1, response.getFailedCount());
        assertFalse(response.getErrorMessages().isEmpty());

        verify(notificationService).createNotification(eq(userId), anyString(), anyString(), eq("cleanup_failed"), anyString());
    }

    @Test
    void testRestoreCleanup_RestoresFilesAndUpdatesHistory() {
        String userId = "user-1";
        String historyId = "hist-1";

        FileItem trashedFile = new FileItem();
        trashedFile.setId("trashed-1");
        trashedFile.setUserId(userId);
        trashedFile.setStorageType("local");
        trashedFile.setStorageConnectionId("conn-1");
        trashedFile.setProviderFileId("C:/restorable.txt");
        trashedFile.setDeleted(true);

        CleanupHistory history = new CleanupHistory();
        history.setId(historyId);
        history.setUserId(userId);
        history.setCleanedFileIds(List.of("trashed-1"));
        history.setRestorationSupported(true);
        history.setRestored(false);

        when(cleanupHistoryRepository.findByIdAndUserId(historyId, userId)).thenReturn(Optional.of(history));
        when(fileItemRepository.findByIdInAndUserId(List.of("trashed-1"), userId)).thenReturn(List.of(trashedFile));
        when(localStorageProvider.restoreFile("conn-1", "C:/restorable.txt")).thenReturn(true);
        when(cleanupHistoryRepository.save(any(CleanupHistory.class))).thenAnswer(i -> i.getArgument(0));

        CleanupHistoryResponse response = cleanupService.restoreCleanup(userId, historyId);

        assertFalse(trashedFile.isDeleted());
        assertNull(trashedFile.getDeletedAt());
        assertTrue(response.isRestored());
        assertNotNull(response.getRestoredAt());

        verify(localStorageProvider).restoreFile("conn-1", "C:/restorable.txt");
    }

    @Test
    void testRestoreCleanup_ThrowsWhenRestorationUnsupported() {
        String userId = "user-1";
        String historyId = "hist-cold";

        CleanupHistory history = new CleanupHistory();
        history.setId(historyId);
        history.setUserId(userId);
        history.setRestorationSupported(false);

        when(cleanupHistoryRepository.findByIdAndUserId(historyId, userId)).thenReturn(Optional.of(history));

        assertThrows(BadRequestException.class, () -> cleanupService.restoreCleanup(userId, historyId));
    }
}
