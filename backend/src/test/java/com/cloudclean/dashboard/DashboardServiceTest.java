package com.cloudclean.dashboard;

import com.cloudclean.cleanup.CleanupHistory;
import com.cloudclean.dto.DashboardMetricsResponse;
import com.cloudclean.file.FileItem;
import com.cloudclean.file.FileService;
import com.cloudclean.recommendation.RecommendationService;
import com.cloudclean.repository.CleanupHistoryRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.ScanJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private FileItemRepository fileItemRepository;
    private FileService fileService;
    private ScanJobRepository scanJobRepository;
    private RecommendationService recommendationService;
    private CleanupHistoryRepository cleanupHistoryRepository;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        fileService = Mockito.mock(FileService.class);
        scanJobRepository = Mockito.mock(ScanJobRepository.class);
        recommendationService = Mockito.mock(RecommendationService.class);
        cleanupHistoryRepository = Mockito.mock(CleanupHistoryRepository.class);

        dashboardService = new DashboardService(
                fileItemRepository,
                fileService,
                scanJobRepository,
                recommendationService,
                cleanupHistoryRepository
        );
    }

    @Test
    void testMetricsDerivedFromActualFiles_AvoidsDoubleCountingOverlappingSignals() {
        String userId = "user-dash";

        // File 1: Canonical Original (Clean / Keep Original)
        FileItem canonicalOriginal = new FileItem();
        canonicalOriginal.setId("f-orig");
        canonicalOriginal.setUserId(userId);
        canonicalOriginal.setSizeBytes(50_000_000L);
        canonicalOriginal.setHash("hash-abc");
        canonicalOriginal.setCategory("duplicate");
        canonicalOriginal.setRecommendationStatus("Keep Original");
        canonicalOriginal.setDeleted(false);

        // File 2: Duplicate Copy
        FileItem duplicateCopy = new FileItem();
        duplicateCopy.setId("f-dup");
        duplicateCopy.setUserId(userId);
        duplicateCopy.setSizeBytes(50_000_000L);
        duplicateCopy.setHash("hash-abc");
        duplicateCopy.setCategory("duplicate");
        duplicateCopy.setRecommendationStatus("Safe to Delete");
        duplicateCopy.setDeleted(false);

        // File 3: Overlapping signals! It is Large (150MB), Inactive, and has high junk score
        FileItem overlapFile = new FileItem();
        overlapFile.setId("f-overlap");
        overlapFile.setUserId(userId);
        overlapFile.setSizeBytes(150_000_000L);
        overlapFile.setCategory("large");
        overlapFile.setInactivityCategory("365_days");
        overlapFile.setJunkScore(70);
        overlapFile.setRecommendationStatus("Review & Delete");
        overlapFile.setDeleted(false);

        List<FileItem> allFiles = List.of(canonicalOriginal, duplicateCopy, overlapFile);
        when(fileItemRepository.findByUserId(userId)).thenReturn(allFiles);

        // Cleanup history: 20MB freed
        CleanupHistory hist = new CleanupHistory(userId, "Cleaned temp", "Temp", 1, 20_000_000L, "User", "Trash");
        hist.setRestored(false);
        when(cleanupHistoryRepository.findByUserIdOrderByTimestampDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(hist)));

        DashboardMetricsResponse metrics = dashboardService.getDashboardMetrics(userId);

        // Total used = 50MB + 50MB + 150MB = 250MB
        assertEquals(250_000_000L, metrics.getTotalUsedBytes());

        // Duplicates: 1 candidate copy (50MB)
        assertEquals(1, metrics.getDuplicateFilesCount());
        assertEquals(50_000_000L, metrics.getDuplicateBytes());

        // Recoverable bytes MUST NOT double count overlapFile!
        // Distinct recoverable files = duplicateCopy (50MB) + overlapFile (150MB) = 200MB!
        // Canonical original must NOT be counted!
        assertEquals(200_000_000L, metrics.getRecoverableBytes());

        // Storage actually recovered = 20MB
        assertEquals(20_000_000L, metrics.getStorageActuallyRecoveredBytes());

        // Total files scanned
        assertEquals(3, metrics.getTotalFilesScanned());
    }
}
