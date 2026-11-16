package com.cloudclean.detection;

import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileAnalysisRepository;
import com.cloudclean.repository.FileItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InactiveFileDetectorTest {

    private FileItemRepository fileItemRepository;
    private FileAnalysisRepository fileAnalysisRepository;
    private InactiveFileDetector detector;

    @BeforeEach
    void setUp() {
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        fileAnalysisRepository = Mockito.mock(FileAnalysisRepository.class);
        detector = new InactiveFileDetector(fileItemRepository, fileAnalysisRepository);
    }

    @Test
    void testInactivityBucketingAndThresholds() {
        String userId = "user-inact";
        Instant now = Instant.now();

        // File 1: 400 days old (365_days bucket)
        FileItem f400 = new FileItem();
        f400.setId("f-400");
        f400.setName("old-archive.tar");
        f400.setSizeBytes(1000L);
        f400.setLastModified(now.minusSeconds(3600L * 24 * 400));
        f400.setLastAccessedReliable(false);

        // File 2: 200 days old (180_days bucket) with reliable access time
        FileItem f200 = new FileItem();
        f200.setId("f-200");
        f200.setName("report-2023.docx");
        f200.setSizeBytes(2000L);
        f200.setLastAccessed(now.minusSeconds(3600L * 24 * 200));
        f200.setLastAccessedReliable(true);

        // File 3: 100 days old (90_days bucket)
        FileItem f100 = new FileItem();
        f100.setId("f-100");
        f100.setName("draft-notes.txt");
        f100.setSizeBytes(300L);
        f100.setLastModified(now.minusSeconds(3600L * 24 * 100));
        f100.setLastAccessedReliable(false);

        // File 4: 10 days old (active)
        FileItem f10 = new FileItem();
        f10.setId("f-10");
        f10.setName("today.txt");
        f10.setSizeBytes(50L);
        f10.setLastModified(now.minusSeconds(3600L * 24 * 10));

        when(fileItemRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(List.of(f400, f200, f100, f10));
        when(fileItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // Evaluate with default threshold 180 days
        InactiveFileDetector.InactivityReport report = detector.detectInactiveFiles(userId, 180);

        assertNotNull(report);
        assertEquals(4, report.totalEvaluated());
        assertEquals(2, report.flaggedInactiveCount()); // f400 and f200 >= 180 days
        assertEquals(3000L, report.potentialInactiveBytes());

        assertEquals("365_days", f400.getInactivityCategory());
        assertEquals("Safe to Delete", f400.getRecommendationStatus());
        assertTrue(f400.getDetectionReason().contains("host access timestamp unavailable"));

        assertEquals("180_days", f200.getInactivityCategory());
        assertEquals("Recommended for Archival", f200.getRecommendationStatus());
        assertTrue(f200.getDetectionReason().contains("unaccessed for 200 days"));

        assertEquals("90_days", f100.getInactivityCategory());
        assertEquals("active", f10.getInactivityCategory());
    }
}
