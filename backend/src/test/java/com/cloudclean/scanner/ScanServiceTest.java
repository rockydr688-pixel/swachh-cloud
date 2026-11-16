package com.cloudclean.scanner;

import com.cloudclean.analysis.ImageAnalysisService;
import com.cloudclean.detection.DuplicateDetectionService;
import com.cloudclean.dto.ScanJobResponse;
import com.cloudclean.dto.ScanRequest;
import com.cloudclean.exception.BadRequestException;
import com.cloudclean.file.FileService;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.ScanJobRepository;
import com.cloudclean.repository.StorageConnectionRepository;
import com.cloudclean.storage.agent.LocalAgentScanner;
import com.cloudclean.storage.provider.StorageProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScanServiceTest {

    private ScanJobRepository scanJobRepository;
    private FileItemRepository fileItemRepository;
    private StorageConnectionRepository connectionRepository;
    private StorageProviderRegistry storageProviderRegistry;
    private LocalAgentScanner localAgentScanner;
    private FileService fileService;
    private DuplicateDetectionService duplicateDetectionService;
    private ImageAnalysisService imageAnalysisService;
    private ScanService scanService;

    @BeforeEach
    void setUp() {
        scanJobRepository = Mockito.mock(ScanJobRepository.class);
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        connectionRepository = Mockito.mock(StorageConnectionRepository.class);
        storageProviderRegistry = Mockito.mock(StorageProviderRegistry.class);
        localAgentScanner = Mockito.mock(LocalAgentScanner.class);
        fileService = Mockito.mock(FileService.class);
        duplicateDetectionService = Mockito.mock(DuplicateDetectionService.class);
        imageAnalysisService = Mockito.mock(ImageAnalysisService.class);

        scanService = new ScanService(
                scanJobRepository,
                fileItemRepository,
                connectionRepository,
                storageProviderRegistry,
                localAgentScanner,
                fileService,
                duplicateDetectionService,
                imageAnalysisService
        );
    }

    @Test
    void testDuplicateScanPrevention_ThrowsBadRequest() {
        String userId = "user-123";
        String target = "Local Computer";

        ScanJob existingActiveJob = new ScanJob(userId, target, "local");
        existingActiveJob.setStatus("in_progress");

        when(scanJobRepository.findByUserIdAndTargetAndStatusIn(
                eq(userId), eq(target), any()
        )).thenReturn(List.of(existingActiveJob));

        ScanRequest request = new ScanRequest();
        request.setTarget(target);
        request.setStorageType("local");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                scanService.triggerScan(userId, request)
        );

        assertTrue(ex.getMessage().contains("already in progress"));
        verify(scanJobRepository, never()).save(any(ScanJob.class));
    }

    @Test
    void testTriggerScan_SuccessWhenNoDuplicate() {
        String userId = "user-123";
        String target = "Local Computer";

        when(scanJobRepository.findByUserIdAndTargetAndStatusIn(
                eq(userId), eq(target), any()
        )).thenReturn(List.of());

        when(scanJobRepository.save(any(ScanJob.class))).thenAnswer(invocation -> {
            ScanJob job = invocation.getArgument(0);
            job.setId("new-scan-id");
            return job;
        });

        ScanRequest request = new ScanRequest();
        request.setTarget(target);
        request.setStorageType("local");

        ScanJobResponse response = scanService.triggerScan(userId, request);

        assertNotNull(response);
        assertEquals("new-scan-id", response.getId());
        assertEquals(target, response.getTarget());
    }

    @Test
    void testCancelScan_Success() {
        String userId = "user-123";
        String scanId = "scan-to-cancel";

        ScanJob activeJob = new ScanJob(userId, "Local Computer", "local");
        activeJob.setId(scanId);
        activeJob.setStatus("in_progress");

        when(scanJobRepository.findByIdAndUserId(scanId, userId)).thenReturn(Optional.of(activeJob));
        when(scanJobRepository.save(any(ScanJob.class))).thenAnswer(inv -> inv.getArgument(0));

        ScanJobResponse response = scanService.cancelScan(userId, scanId);

        assertNotNull(response);
        assertEquals("cancelled", response.getStatus());
        assertNotNull(response.getCompletedTime());
        verify(scanJobRepository, times(1)).save(activeJob);
    }
}

