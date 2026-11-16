package com.cloudclean.detection;

import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileAnalysisRepository;
import com.cloudclean.repository.FileItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class JunkScoringEngineTest {

    private FileItemRepository fileItemRepository;
    private FileAnalysisRepository fileAnalysisRepository;
    private JunkScoringEngine engine;

    @BeforeEach
    void setUp() {
        fileItemRepository = Mockito.mock(FileItemRepository.class);
        fileAnalysisRepository = Mockito.mock(FileAnalysisRepository.class);
        engine = new JunkScoringEngine(fileItemRepository, fileAnalysisRepository);
    }

    @Test
    void testAvoidsDoubleCountingOnCanonicalOriginal() {
        FileItem canonicalOriginal = new FileItem();
        canonicalOriginal.setCategory("duplicate");
        canonicalOriginal.setRecommendationStatus("Keep Original");
        canonicalOriginal.setSizeBytes(5000L);

        JunkScoringEngine.JunkEvaluation eval = engine.scoreFile(canonicalOriginal);

        // Canonical original must NOT get +30 duplicate junk points
        assertEquals(0, eval.junkScore());
        assertEquals("high", eval.riskCategory());
        assertEquals("Keep Original", eval.recommendedAction());
    }

    @Test
    void testDuplicateCopyReceivesFullPoints() {
        FileItem duplicateCopy = new FileItem();
        duplicateCopy.setCategory("duplicate");
        duplicateCopy.setRecommendationStatus("Safe to Delete");
        duplicateCopy.setSizeBytes(5000L);

        JunkScoringEngine.JunkEvaluation eval = engine.scoreFile(duplicateCopy);

        assertTrue(eval.junkScore() >= 30);
        assertEquals("low", eval.riskCategory());
        assertEquals("Safe to Delete", eval.recommendedAction());
    }

    @Test
    void testCompositeScoringCappedAt100() {
        FileItem junkFile = new FileItem();
        junkFile.setCategory("duplicate");
        junkFile.setRecommendationStatus("Safe to Delete");
        junkFile.setSizeBytes(700L * 1024 * 1024); // Large footprint (+10)
        junkFile.setBlurScore(0.2); // Blurry (+16)
        junkFile.setInactivityCategory("365_days"); // Inactive (+15)
        junkFile.setTemporary(true); // Temporary (+25)

        JunkScoringEngine.JunkEvaluation eval = engine.scoreFile(junkFile);

        // Total additive points: 30 (duplicate) + 10 (large) + 16 (blurry) + 15 (inactive) + 25 (temp) = 96
        assertTrue(eval.junkScore() >= 70);
        assertTrue(eval.junkScore() <= 100, "Score must never exceed 100");
    }

    @Test
    void testRiskAndRecommendationTiers() {
        // High junk score -> Safe to Delete
        FileItem itemA = new FileItem();
        itemA.setTemporary(true);
        itemA.setInactivityCategory("365_days");
        itemA.setCategory("temporary");

        JunkScoringEngine.JunkEvaluation evalA = engine.scoreFile(itemA);
        assertTrue(evalA.junkScore() >= 40);
        assertNotEquals("Keep Original", evalA.recommendedAction());
    }
}
