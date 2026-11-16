package com.cloudclean.report;

import com.cloudclean.cleanup.CleanupHistory;
import com.cloudclean.file.FileItem;
import com.cloudclean.repository.CleanupHistoryRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.ScanJobRepository;
import com.cloudclean.scanner.ScanJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private FileItemRepository fileItemRepository;
    private CleanupHistoryRepository cleanupHistoryRepository;
    private ScanJobRepository scanJobRepository;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        cleanupHistoryRepository = Mockito.mock(CleanupHistoryRepository.class);
        scanJobRepository = Mockito.mock(ScanJobRepository.class);

        reportService = new ReportService(fileItemRepository, cleanupHistoryRepository, scanJobRepository);
    }

    @Test
    void testGetReportsOverview_CalculatesAccurateTotals() {
        String userId = "user-123";

        FileItem file1 = new FileItem();
        file1.setSizeBytes(10_000_000L);
        file1.setCategory("duplicate");
        file1.setFileType("image");

        FileItem file2 = new FileItem();
        file2.setSizeBytes(25_000_000L);
        file2.setCategory("large");
        file2.setFileType("video");

        when(fileItemRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(List.of(file1, file2));

        CleanupHistory cleanup = new CleanupHistory(userId, "Purged files", "Duplicates", 1, 10_000_000L, "User", "Trash");
        Page<CleanupHistory> cleanupPage = new PageImpl<>(List.of(cleanup));
        when(cleanupHistoryRepository.findByUserIdOrderByTimestampDesc(eq(userId), any(Pageable.class))).thenReturn(cleanupPage);

        ScanJob scan = new ScanJob(userId, "Local Computer", "local");
        when(scanJobRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(scan));

        Map<String, Object> overview = reportService.getReportsOverview(userId);

        assertEquals(2, overview.get("totalFiles"));
        assertEquals(35_000_000L, overview.get("totalUsedBytes"));
        assertEquals(10_000_000L, overview.get("totalCleanedBytes"));
        assertEquals(1, overview.get("totalScansExecuted"));
    }

    @Test
    void testExportFilesToCsv_ProducesStandardHeaderAndRows() {
        String userId = "user-123";
        FileItem file = new FileItem();
        file.setId("f-1");
        file.setName("presentation.pptx");
        file.setFileType("document");
        file.setCategory("inactive");
        file.setSizeBytes(5000L);
        file.setStorageType("local");
        file.setStorageLocation("C:/docs/presentation.pptx");
        file.setLastModified(Instant.now());
        file.setJunkScore(40);
        file.setRecommendationStatus("Review & Delete");
        file.setProtected(false);

        when(fileItemRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(List.of(file));

        String csv = reportService.exportFilesToCsv(userId);

        assertTrue(csv.contains("ID,Name,Type,Category,SizeBytes"));
        assertTrue(csv.contains("presentation.pptx"));
        assertTrue(csv.contains("Review & Delete"));
    }

    @Test
    void testExportCleanupHistoryToCsv_ProducesAuditRows() {
        String userId = "user-123";
        CleanupHistory c = new CleanupHistory(userId, "Removed Duplicate Video", "large", 1, 500_000_000L, "Tester", "Trash");
        c.setId("clean-1");
        c.setRestorationSupported(true);
        c.setRestored(false);

        Page<CleanupHistory> cleanupPage = new PageImpl<>(List.of(c));
        when(cleanupHistoryRepository.findByUserIdOrderByTimestampDesc(eq(userId), any(Pageable.class))).thenReturn(cleanupPage);

        String csv = reportService.exportCleanupHistoryToCsv(userId);

        assertTrue(csv.contains("ID,Timestamp,Action,Category,FilesAffected,FreedSpaceBytes"));
        assertTrue(csv.contains("Removed Duplicate Video"));
    }
}
