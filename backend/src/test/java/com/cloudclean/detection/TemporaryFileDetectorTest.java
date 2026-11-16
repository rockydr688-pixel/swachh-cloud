package com.cloudclean.detection;

import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileAnalysisRepository;
import com.cloudclean.repository.FileItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TemporaryFileDetectorTest {

    private FileItemRepository fileItemRepository;
    private FileAnalysisRepository fileAnalysisRepository;
    private TemporaryFileDetector detector;

    @BeforeEach
    void setUp() {
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        fileAnalysisRepository = Mockito.mock(FileAnalysisRepository.class);
        detector = new TemporaryFileDetector(fileItemRepository, fileAnalysisRepository);
    }

    @Test
    void testSystemJunkArtifact_ClassifiedSafeToDelete() {
        FileItem thumbs = new FileItem();
        thumbs.setName("Thumbs.db");
        thumbs.setExtension("db");
        thumbs.setStorageLocation("C:/Users/rocky/Pictures/Thumbs.db");
        thumbs.setLastModified(Instant.now().minusSeconds(3600 * 48));

        TemporaryFileDetector.TempFileEvaluation eval = detector.evaluateFile(thumbs);

        assertTrue(eval.isTemporary());
        assertEquals("Safe to Delete", eval.recommendedAction());
        assertEquals("low", eval.riskCategory());
        assertTrue(eval.confidence() >= 0.90);
    }

    @Test
    void testStaleTempFileInTempDir_ClassifiedSafeToDelete() {
        FileItem tempFile = new FileItem();
        tempFile.setName("installer_buffer.tmp");
        tempFile.setExtension("tmp");
        tempFile.setStorageLocation("C:/Users/rocky/AppData/Local/Temp/installer_buffer.tmp");
        tempFile.setLastModified(Instant.now().minusSeconds(3600 * 72)); // 3 days ago

        TemporaryFileDetector.TempFileEvaluation eval = detector.evaluateFile(tempFile);

        assertTrue(eval.isTemporary());
        assertEquals("Safe to Delete", eval.recommendedAction());
        assertEquals("low", eval.riskCategory());
    }

    @Test
    void testRecentlyModifiedTempFile_SafetyRule_NotSafeToDelete() {
        // SAFETY RULE TEST: File modified 20 minutes ago might be in active use by an editor/compiler
        FileItem activeSwp = new FileItem();
        activeSwp.setName(".main.py.swp");
        activeSwp.setExtension("swp");
        activeSwp.setStorageLocation("C:/Projects/app/.main.py.swp");
        activeSwp.setLastModified(Instant.now().minusSeconds(1200)); // 20 mins ago

        TemporaryFileDetector.TempFileEvaluation eval = detector.evaluateFile(activeSwp);

        assertTrue(eval.isTemporary());
        // Must NOT be safe to delete because it was recently modified!
        assertNotEquals("Safe to Delete", eval.recommendedAction());
        assertEquals("Keep Original", eval.recommendedAction());
        assertEquals("high", eval.riskCategory());
    }

    @Test
    void testExtensionOnlyInUserFolder_SafetyRule_RequiresReview() {
        // SAFETY RULE TEST: Old .bak in user folder requires review, not automatic safe-to-delete
        FileItem bakFile = new FileItem();
        bakFile.setName("database-backup.bak");
        bakFile.setExtension("bak");
        bakFile.setStorageLocation("C:/Users/rocky/Documents/database-backup.bak");
        bakFile.setLastModified(Instant.now().minusSeconds(3600 * 24 * 10)); // 10 days ago

        TemporaryFileDetector.TempFileEvaluation eval = detector.evaluateFile(bakFile);

        assertTrue(eval.isTemporary());
        // Must be "Review & Delete", never automatically "Safe to Delete"
        assertEquals("Review & Delete", eval.recommendedAction());
        assertEquals("medium", eval.riskCategory());
    }
}
