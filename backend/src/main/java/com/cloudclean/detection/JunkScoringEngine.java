package com.cloudclean.detection;

import com.cloudclean.analysis.FileAnalysis;
import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileAnalysisRepository;
import com.cloudclean.repository.FileItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JunkScoringEngine {

    private static final Logger log = LoggerFactory.getLogger(JunkScoringEngine.class);

    // Initial illustrative weights (Max sum = 100)
    public static final int WEIGHT_DUPLICATE = 30;
    public static final int WEIGHT_BLURRY = 20;
    public static final int WEIGHT_INACTIVE = 15;
    public static final int WEIGHT_LARGE = 10;
    public static final int WEIGHT_TEMPORARY = 25;

    private final FileItemRepository fileItemRepository;
    private final FileAnalysisRepository fileAnalysisRepository;

    public JunkScoringEngine(FileItemRepository fileItemRepository,
                             FileAnalysisRepository fileAnalysisRepository) {
        this.fileItemRepository = fileItemRepository;
        this.fileAnalysisRepository = fileAnalysisRepository;
    }

    public record JunkEvaluation(
            int junkScore,
            String riskCategory, // "low" | "medium" | "high"
            String recommendedAction, // "Safe to Delete" | "Review & Delete" | "Recommended for Archival" | "Keep Original"
            List<String> detectionReasons,
            Map<String, Object> evidence
    ) {}

    /**
     * Evaluates a file item using additive weighted signals while avoiding double-counting.
     */
    public JunkEvaluation scoreFile(FileItem file) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        Map<String, Object> evidence = new HashMap<>();

        // Signal 1: Duplicate File Detection (30 pts)
        // Canonical original must NOT receive duplicate junk points!
        if ("duplicate".equalsIgnoreCase(file.getCategory())) {
            if ("Keep Original".equalsIgnoreCase(file.getRecommendationStatus())) {
                evidence.put("duplicate_status", "canonical_original");
            } else {
                score += WEIGHT_DUPLICATE;
                reasons.add("Exact SHA-256 duplicate (+30 pts)");
                evidence.put("duplicate_points", WEIGHT_DUPLICATE);
                evidence.put("sha256", file.getHash());
            }
        }

        // Signal 2: Blurry Image Evaluation (20 pts)
        if ("blurry".equalsIgnoreCase(file.getCategory()) || (file.getBlurScore() != null && file.getBlurScore() < 0.70)) {
            double blurScore = file.getBlurScore() != null ? file.getBlurScore() : 0.4;
            int blurPts = (int) Math.round(WEIGHT_BLURRY * (1.0 - blurScore));
            blurPts = Math.max(5, Math.min(WEIGHT_BLURRY, blurPts));
            score += blurPts;
            reasons.add(String.format("Defective blur / low clarity score %.2f (+%d pts)", blurScore, blurPts));
            evidence.put("blur_points", blurPts);
            evidence.put("blur_score", blurScore);
        }

        // Signal 3: Inactivity Age (15 pts)
        String inactCat = file.getInactivityCategory();
        if (inactCat != null && !"active".equals(inactCat)) {
            int inactPts = switch (inactCat) {
                case "365_days" -> WEIGHT_INACTIVE;
                case "180_days" -> 10;
                case "90_days" -> 5;
                default -> 3;
            };
            score += inactPts;
            reasons.add(String.format("Dormant file (%s, +%d pts)", inactCat, inactPts));
            evidence.put("inactive_points", inactPts);
            evidence.put("inactivity_category", inactCat);
        }

        // Signal 4: Large File Footprint (10 pts)
        if ("large".equalsIgnoreCase(file.getCategory()) || file.getSizeBytes() >= 100L * 1024 * 1024) {
            score += WEIGHT_LARGE;
            reasons.add(String.format("Substantial storage footprint: %d MB (+10 pts)", file.getSizeBytes() / (1024 * 1024)));
            evidence.put("large_points", WEIGHT_LARGE);
            evidence.put("size_bytes", file.getSizeBytes());
        }

        // Signal 5: Temporary / OS Junk (25 pts)
        if (file.isTemporary() || "temporary".equalsIgnoreCase(file.getCategory())) {
            score += WEIGHT_TEMPORARY;
            reasons.add("Temporary file or cache artifact (+25 pts)");
            evidence.put("temporary_points", WEIGHT_TEMPORARY);
        }

        // Bound to maximum 100 points
        int finalScore = Math.min(100, score);
        evidence.put("raw_score", score);
        evidence.put("final_score", finalScore);

        // Determine Risk Category
        String riskCategory = determineRiskCategory(file, finalScore);

        // Determine Recommended Action
        String recommendedAction = determineRecommendedAction(file, finalScore, riskCategory);

        return new JunkEvaluation(finalScore, riskCategory, recommendedAction, reasons, evidence);
    }

    /**
     * Re-scores all files for a user and stores comprehensive analysis results.
     */
    public List<FileItem> scoreAllUserFiles(String userId) {
        List<FileItem> allFiles = fileItemRepository.findByUserIdAndIsDeletedFalse(userId);
        List<FileAnalysis> analyses = new ArrayList<>();

        for (FileItem file : allFiles) {
            JunkEvaluation eval = scoreFile(file);
            file.setJunkScore(eval.junkScore());
            file.setRiskCategory(eval.riskCategory());
            file.setEvidence(eval.evidence());

            // If not canonical original, align recommendation
            if (!"Keep Original".equalsIgnoreCase(file.getRecommendationStatus())) {
                file.setRecommendationStatus(eval.recommendedAction());
            }

            FileAnalysis analysis = new FileAnalysis(
                    userId,
                    file.getId(),
                    "junk_score",
                    eval.junkScore(),
                    eval.junkScore() >= 30,
                    String.join("; ", eval.detectionReasons())
            );
            analysis.setJunkScore(eval.junkScore());
            analysis.setRiskCategory(eval.riskCategory());
            analysis.setRecommendedAction(eval.recommendedAction());
            analysis.setMetrics(eval.evidence());

            analyses.add(analysis);
        }

        fileAnalysisRepository.saveAll(analyses);
        return fileItemRepository.saveAll(allFiles);
    }

    private String determineRiskCategory(FileItem file, int score) {
        // High risk if canonical original or recent file
        if ("Keep Original".equalsIgnoreCase(file.getRecommendationStatus())) {
            return "high";
        }
        if (file.isTemporary() && "Keep Original".equalsIgnoreCase(file.getRecommendationStatus())) {
            return "high";
        }
        // Low risk if verified exact duplicate copy or stale temporary OS file
        if (("duplicate".equalsIgnoreCase(file.getCategory()) && !"Keep Original".equalsIgnoreCase(file.getRecommendationStatus()))
                || ("temporary".equalsIgnoreCase(file.getCategory()) && score >= 50)) {
            return "low";
        }
        return "medium";
    }

    private String determineRecommendedAction(FileItem file, int score, String riskCategory) {
        if ("Keep Original".equalsIgnoreCase(file.getRecommendationStatus())) {
            return "Keep Original";
        }
        if ("duplicate".equalsIgnoreCase(file.getCategory()) && "low".equalsIgnoreCase(riskCategory)) {
            return "Safe to Delete";
        }
        if (score >= 50) {
            return "Safe to Delete";
        }
        if (score >= 25) {
            return "Review & Delete";
        }
        if (score >= 15) {
            return "Recommended for Archival";
        }
        return "Keep Original";
    }
}
