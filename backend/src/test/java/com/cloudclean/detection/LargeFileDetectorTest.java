package com.cloudclean.detection;

import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileAnalysisRepository;
import com.cloudclean.repository.FileItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LargeFileDetectorTest {

    private FileItemRepository fileItemRepository;
    private FileAnalysisRepository fileAnalysisRepository;
    private LargeFileDetector detector;

    @BeforeEach
    void setUp() {
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        fileAnalysisRepository = Mockito.mock(FileAnalysisRepository.class);
        detector = new LargeFileDetector(fileItemRepository, fileAnalysisRepository);
    }

    @Test
    void testCategorizedLargeFileThresholds() {
        String userId = "user-large";

        // Video of 600 MB (exceeds 500MB default video threshold)
        FileItem bigVideo = new FileItem();
        bigVideo.setId("v-1");
        bigVideo.setName("screen-recording.mp4");
        bigVideo.setFileType("video");
        bigVideo.setSizeBytes(600L * 1024 * 1024);

        // Small video of 150 MB (does not exceed threshold)
        FileItem smallVideo = new FileItem();
        smallVideo.setId("v-2");
        smallVideo.setName("clip.mp4");
        smallVideo.setFileType("video");
        smallVideo.setSizeBytes(150L * 1024 * 1024);

        // Image of 35 MB (exceeds 25MB default image threshold)
        FileItem bigImage = new FileItem();
        bigImage.setId("img-1");
        bigImage.setName("raw-panorama.tiff");
        bigImage.setFileType("image");
        bigImage.setSizeBytes(35L * 1024 * 1024);

        // Archive of 2 GB (exceeds 1GB default archive threshold)
        FileItem bigArchive = new FileItem();
        bigArchive.setId("arc-1");
        bigArchive.setName("backup-2023.tar.gz");
        bigArchive.setFileType("archive");
        bigArchive.setSizeBytes(2000L * 1024 * 1024);

        when(fileItemRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(List.of(bigVideo, smallVideo, bigImage, bigArchive));
        when(fileItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        LargeFileDetector.LargeFileReport report = detector.detectLargeFiles(userId, null);

        assertNotNull(report);
        assertEquals(4, report.totalEvaluated());
        assertEquals(3, report.flaggedLargeCount()); // bigVideo, bigImage, bigArchive
        assertEquals("large", bigVideo.getCategory());
        assertEquals("large", bigImage.getCategory());
        assertEquals("large", bigArchive.getCategory());
        assertEquals("clean", smallVideo.getCategory());
    }

    @Test
    void testCustomThresholdConfiguration() {
        String userId = "user-custom-large";

        // Image of 15 MB
        FileItem img = new FileItem();
        img.setId("img-custom");
        img.setName("photo.raw");
        img.setFileType("image");
        img.setSizeBytes(15L * 1024 * 1024);

        when(fileItemRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(List.of(img));
        when(fileItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // Under default threshold of 25MB, it should NOT be flagged
        LargeFileDetector.LargeFileReport defReport = detector.detectLargeFiles(userId, LargeFileDetector.LargeFileThresholds.defaults());
        assertEquals(0, defReport.flaggedLargeCount());

        // Under custom threshold of 10MB, it SHOULD be flagged
        LargeFileDetector.LargeFileThresholds custom = new LargeFileDetector.LargeFileThresholds(
                10L * 1024 * 1024, 500L * 1024 * 1024, 1024L * 1024 * 1024, 100L * 1024 * 1024, 500L * 1024 * 1024
        );
        LargeFileDetector.LargeFileReport customReport = detector.detectLargeFiles(userId, custom);
        assertEquals(1, customReport.flaggedLargeCount());
        assertEquals("large", img.getCategory());
    }
}
