package com.cloudclean.detection;

import com.cloudclean.file.FileItem;
import com.cloudclean.repository.DuplicateGroupRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.storage.provider.StorageProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DuplicateDetectorTest {

    private DuplicateGroupRepository duplicateGroupRepository;
    private FileItemRepository fileItemRepository;
    private StorageProviderRegistry storageProviderRegistry;
    private DuplicateDetector detector;

    @BeforeEach
    void setUp() {
        duplicateGroupRepository = Mockito.mock(DuplicateGroupRepository.class);
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        storageProviderRegistry = Mockito.mock(StorageProviderRegistry.class);
        detector = new DuplicateDetector(duplicateGroupRepository, fileItemRepository, storageProviderRegistry);
    }

    @Test
    void testSizeFirstOptimization_SkipsUniqueSizes() {
        String userId = "user-1";

        // File 1 has unique size 100 bytes
        FileItem uniqueFile = new FileItem();
        uniqueFile.setId("f-unique");
        uniqueFile.setSizeBytes(100L);
        uniqueFile.setName("unique.txt");

        // Files 2 and 3 have duplicate size 500 bytes and identical hash
        FileItem f2 = new FileItem();
        f2.setId("f-2");
        f2.setSizeBytes(500L);
        f2.setName("doc-v1.pdf");
        f2.setHash("sha256-abc");
        f2.setCreatedTime(Instant.now().minusSeconds(1000));

        FileItem f3 = new FileItem();
        f3.setId("f-3");
        f3.setSizeBytes(500L);
        f3.setName("doc-v2.pdf");
        f3.setHash("sha256-abc");
        f3.setCreatedTime(Instant.now().minusSeconds(500));

        when(fileItemRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(List.of(uniqueFile, f2, f3));
        when(duplicateGroupRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        DuplicateDetector.DuplicateDetectionReport report = detector.detectDuplicates(userId);

        assertNotNull(report);
        assertEquals(1, report.duplicateGroups().size());
        assertEquals(1, report.totalDuplicateFiles());
        assertEquals(500L, report.totalRecoverableBytes()); // Recoverable storage for 1 copy without double counting

        // Verify canonical original vs copy
        assertEquals("Keep Original", f2.getRecommendationStatus());
        assertEquals("Safe to Delete", f3.getRecommendationStatus());
    }

    @Test
    void testCachedHashReuse_AvoidsRepeatedHashing() {
        String userId = "user-2";

        FileItem f1 = new FileItem();
        f1.setId("f-1");
        f1.setSizeBytes(2000L);
        f1.setHash("trusted-cached-hash-123");
        f1.setCreatedTime(Instant.now().minusSeconds(3600));

        FileItem f2 = new FileItem();
        f2.setId("f-2");
        f2.setSizeBytes(2000L);
        f2.setHash("trusted-cached-hash-123");
        f2.setCreatedTime(Instant.now().minusSeconds(1800));

        when(fileItemRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(List.of(f1, f2));
        when(duplicateGroupRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        DuplicateDetector.DuplicateDetectionReport report = detector.detectDuplicates(userId);

        assertEquals(1, report.duplicateGroups().size());
        // Storage provider was never asked to stream/recalculate because hash was trustworthy
        verify(storageProviderRegistry, never()).getProvider(any());
    }

    @Test
    void testPerceptualHashSeparation_DoesNotConfuseWithExactDuplicates() {
        String userId = "user-3";

        // Two visually similar images with DIFFERENT file sizes and DIFFERENT binary content
        FileItem imgA = new FileItem();
        imgA.setId("img-a");
        imgA.setName("sunset-highres.jpg");
        imgA.setFileType("image");
        imgA.setSizeBytes(5000000L);
        imgA.setHash("hash-a-different");
        imgA.setPerceptualHash("ffff0000ffff0000");

        FileItem imgB = new FileItem();
        imgB.setId("img-b");
        imgB.setName("sunset-compressed.jpg");
        imgB.setFileType("image");
        imgB.setSizeBytes(1200000L);
        imgB.setHash("hash-b-different");
        imgB.setPerceptualHash("ffff0000ffff0001"); // Hamming distance 1 (visually almost identical)

        when(fileItemRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(List.of(imgA, imgB));
        when(duplicateGroupRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // 1. Exact duplicate detection should find ZERO duplicates (different sizes and hashes)
        DuplicateDetector.DuplicateDetectionReport dupReport = detector.detectDuplicates(userId);
        assertEquals(0, dupReport.duplicateGroups().size());
        assertEquals(0, dupReport.totalDuplicateFiles());

        // 2. Perceptual similarity detection should find 1 similar cluster
        List<DuplicateDetector.SimilarImageGroup> similarGroups = detector.detectVisuallySimilarImages(userId, 5);
        assertEquals(1, similarGroups.size());
        assertEquals(imgA.getId(), similarGroups.get(0).primaryImageId());
        assertTrue(similarGroups.get(0).similarImageIds().contains(imgB.getId()));
        assertEquals(1, similarGroups.get(0).distance());
    }
}
