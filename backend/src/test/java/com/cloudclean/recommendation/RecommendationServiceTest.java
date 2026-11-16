package com.cloudclean.recommendation;

import com.cloudclean.audit.AuditLogService;
import com.cloudclean.dto.RecommendationResponse;
import com.cloudclean.file.FileItem;
import com.cloudclean.notification.NotificationService;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {

    private RecommendationRepository recommendationRepository;
    private FileItemRepository fileItemRepository;
    private NotificationService notificationService;
    private AuditLogService auditLogService;
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationRepository = Mockito.mock(RecommendationRepository.class);
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        notificationService = Mockito.mock(NotificationService.class);
        auditLogService = Mockito.mock(AuditLogService.class);

        recommendationService = new RecommendationService(
                recommendationRepository,
                fileItemRepository,
                notificationService,
                auditLogService
        );
    }

    @Test
    void testAcceptRecommendation_UpdatesStatusToAccepted() {
        String userId = "user-123";
        String recId = "rec-1";

        Recommendation rec = new Recommendation();
        rec.setId(recId);
        rec.setUserId(userId);
        rec.setStatus("pending");
        rec.setTitle("Delete redundant file");

        when(recommendationRepository.findByIdAndUserId(recId, userId)).thenReturn(Optional.of(rec));
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(i -> i.getArgument(0));

        RecommendationResponse response = recommendationService.acceptRecommendation(userId, recId);

        assertEquals("accepted", response.getStatus());
        verify(auditLogService).logAction(eq(userId), eq("ACCEPT_RECOMMENDATION"), eq("recommendation"), eq(recId), anyString(), eq(true));
    }

    @Test
    void testRejectRecommendation_UpdatesStatusToRejected() {
        String userId = "user-123";
        String recId = "rec-2";

        Recommendation rec = new Recommendation();
        rec.setId(recId);
        rec.setUserId(userId);
        rec.setStatus("pending");
        rec.setTitle("Purge blurry photo");

        when(recommendationRepository.findByIdAndUserId(recId, userId)).thenReturn(Optional.of(rec));
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(i -> i.getArgument(0));

        RecommendationResponse response = recommendationService.rejectRecommendation(userId, recId);

        assertEquals("rejected", response.getStatus());
        verify(auditLogService).logAction(eq(userId), eq("REJECT_RECOMMENDATION"), eq("recommendation"), eq(recId), anyString(), eq(true));
    }

    @Test
    void testProtectFile_WhitelistsFileAndResetsJunkScore() {
        String userId = "user-123";
        String fileId = "file-99";

        FileItem file = new FileItem();
        file.setId(fileId);
        file.setUserId(userId);
        file.setName("tax_return_2025.pdf");
        file.setJunkScore(45);
        file.setRecommendationStatus("Review & Delete");
        file.setRiskCategory("medium");

        when(fileItemRepository.findByIdAndUserId(fileId, userId)).thenReturn(Optional.of(file));
        when(fileItemRepository.save(any(FileItem.class))).thenAnswer(i -> i.getArgument(0));

        FileItem protectedFile = recommendationService.protectFile(userId, fileId);

        assertTrue(protectedFile.isProtected());
        assertEquals(0, protectedFile.getJunkScore());
        assertEquals("Keep Original", protectedFile.getRecommendationStatus());
        assertEquals("high", protectedFile.getRiskCategory());

        verify(auditLogService).logAction(eq(userId), eq("PROTECT_FILE"), eq("file"), eq(fileId), anyString(), eq(true));
    }

    @Test
    void testGenerateRecommendationsFromDetections_IncludesMandatoryFields() {
        String userId = "user-123";

        FileItem flaggedFile = new FileItem();
        flaggedFile.setId("flagged-1");
        flaggedFile.setUserId(userId);
        flaggedFile.setName("blurry_sunset.jpg");
        flaggedFile.setFileType("image");
        flaggedFile.setSizeBytes(15_000_000L);
        flaggedFile.setStorageType("local");
        flaggedFile.setCategory("blurry");
        flaggedFile.setJunkScore(65);
        flaggedFile.setDetectionReason("Defective blur variance score 0.22");
        flaggedFile.setRecommendationStatus("Review & Delete");
        flaggedFile.setRiskCategory("medium");
        flaggedFile.setUpdatedAt(Instant.now());

        when(fileItemRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(List.of(flaggedFile));
        when(recommendationRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<Recommendation> recs = recommendationService.generateRecommendationsFromDetections(userId);

        assertFalse(recs.isEmpty());
        Recommendation itemRec = recs.stream().filter(r -> "flagged-1".equals(r.getFileId())).findFirst().orElse(null);
        assertNotNull(itemRec);

        // Verification of all mandatory fields
        assertEquals("blurry_sunset.jpg", itemRec.getFileName());
        assertEquals("image", itemRec.getFileType());
        assertEquals(15_000_000L, itemRec.getFileSize());
        assertEquals("local", itemRec.getStorageSource());
        assertEquals(65, itemRec.getJunkScore());
        assertFalse(itemRec.getDetectionReasons().isEmpty());
        assertEquals("Review & Delete", itemRec.getRecommendedAction());
        assertEquals("medium", itemRec.getRiskLevel());
        assertNotNull(itemRec.getAnalysisDate());
    }
}
