package com.cloudclean.detection;

import com.cloudclean.analysis.FileAnalysis;
import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileAnalysisRepository;
import com.cloudclean.repository.FileItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class TemporaryFileDetector {

    private static final Logger log = LoggerFactory.getLogger(TemporaryFileDetector.class);

    // Common temporary extensions
    private static final Set<String> TEMP_EXTENSIONS = Set.of(
            "tmp", "temp", "bak", "old", "swp", "swo", "dmp", "cache", "crdownload", "part"
    );

    // OS / System metadata junk names
    private static final Set<String> SYSTEM_JUNK_NAMES = Set.of(
            "thumbs.db", ".ds_store", "desktop.ini", "$recycle.bin"
    );

    // Known temporary directory path markers
    private static final List<String> TEMP_DIR_PATTERNS = List.of(
            "/tmp/", "\\temp\\", "/temp/", "\\tmp\\",
            "appdata/local/temp", "appdata\\local\\temp",
            ".cache", "node_modules/.cache", "node_modules\\.cache",
            "target/tmp", "__pycache__"
    );

    private final FileItemRepository fileItemRepository;
    private final FileAnalysisRepository fileAnalysisRepository;

    public TemporaryFileDetector(FileItemRepository fileItemRepository,
                                 FileAnalysisRepository fileAnalysisRepository) {
        this.fileItemRepository = fileItemRepository;
        this.fileAnalysisRepository = fileAnalysisRepository;
    }

    public record TempFileEvaluation(
            boolean isTemporary,
            double confidence,
            String reason,
            String recommendedAction,
            String riskCategory,
            Map<String, Object> evidence
    ) {}

    public record TemporaryReport(
            int totalEvaluated,
            int flaggedTempCount,
            long totalTempBytes,
            List<FileAnalysis> analyses
    ) {}

    public TemporaryReport detectTemporaryFiles(String userId) {
        List<FileItem> allFiles = fileItemRepository.findByUserIdAndIsDeletedFalse(userId);
        List<FileAnalysis> analyses = new ArrayList<>();
        int flaggedCount = 0;
        long tempBytes = 0L;

        for (FileItem file : allFiles) {
            TempFileEvaluation eval = evaluateFile(file);

            if (eval.isTemporary()) {
                flaggedCount++;
                tempBytes += file.getSizeBytes();
                file.setTemporary(true);

                // Set category only if not already categorized as duplicate or blurry
                if ("clean".equals(file.getCategory())) {
                    file.setCategory("temporary");
                    file.setRecommendationStatus(eval.recommendedAction());
                    file.setDetectionReason(eval.reason());
                }
            }

            FileAnalysis analysis = new FileAnalysis(
                    userId,
                    file.getId(),
                    "temporary",
                    eval.confidence(),
                    eval.isTemporary(),
                    eval.reason()
            );

            analysis.setRiskCategory(eval.riskCategory());
            analysis.setRecommendedAction(eval.recommendedAction());
            analysis.setMetrics(eval.evidence());

            analyses.add(analysis);
        }

        fileItemRepository.saveAll(allFiles);
        fileAnalysisRepository.saveAll(analyses);

        log.info("Temporary file evaluation finished for user {}: {} files analyzed, {} flagged, {} bytes",
                userId, allFiles.size(), flaggedCount, tempBytes);

        return new TemporaryReport(allFiles.size(), flaggedCount, tempBytes, analyses);
    }

    /**
     * Strict multi-factor evaluation enforcing the rule:
     * Never automatically classify a file as safe to delete solely because of its extension.
     */
    public TempFileEvaluation evaluateFile(FileItem file) {
        String name = file.getName() != null ? file.getName().toLowerCase() : "";
        String ext = file.getExtension() != null ? file.getExtension().toLowerCase() : "";
        String path = file.getStorageLocation() != null ? file.getStorageLocation().toLowerCase() : "";

        boolean hasTempExt = TEMP_EXTENSIONS.contains(ext) || name.startsWith("~") || name.endsWith(".bak");
        boolean isSystemJunk = SYSTEM_JUNK_NAMES.contains(name);
        boolean inTempDir = matchesTempDirectory(path);

        // Check file recency (files modified less than 2 hours ago might be active editor buffers)
        Instant modified = file.getLastModified() != null ? file.getLastModified() : file.getCreatedTime();
        boolean isRecentlyModified = false;
        long hoursOld = 9999;
        if (modified != null) {
            hoursOld = Duration.between(modified, Instant.now()).toHours();
            isRecentlyModified = hoursOld < 2;
        }

        Map<String, Object> evidence = new HashMap<>();
        evidence.put("has_temp_extension", hasTempExt);
        evidence.put("is_system_junk", isSystemJunk);
        evidence.put("in_temp_directory", inTempDir);
        evidence.put("hours_old", hoursOld);
        evidence.put("recently_modified", isRecentlyModified);

        // Case 1: In temporary directory AND temporary extension AND not active
        if (inTempDir && (hasTempExt || isSystemJunk) && !isRecentlyModified) {
            evidence.put("confidence", 0.95);
            return new TempFileEvaluation(
                    true,
                    0.95,
                    String.format("Stale temp file in temporary directory (%s, modified %d hours ago)", ext, hoursOld),
                    "Safe to Delete",
                    "low",
                    evidence
            );
        }

        // Case 2: System junk metadata file (e.g. Thumbs.db, .DS_Store)
        if (isSystemJunk) {
            evidence.put("confidence", 0.90);
            return new TempFileEvaluation(
                    true,
                    0.90,
                    "OS artifact / thumbnail cache (" + file.getName() + ")",
                    "Safe to Delete",
                    "low",
                    evidence
            );
        }

        // Case 3: Temporary extension in user folder, but recently modified
        // STRICT SAFETY CHECK: Never mark recently modified file as safe to delete!
        if (hasTempExt && isRecentlyModified) {
            evidence.put("confidence", 0.40);
            return new TempFileEvaluation(
                    true,
                    0.40,
                    String.format("Recent temporary buffer (.%s) modified %d hours ago (may be in active use)", ext, hoursOld),
                    "Keep Original",
                    "high",
                    evidence
            );
        }

        // Case 4: Temporary extension in user folder, aged > 24 hours
        // Must NOT automatically classify as safe to delete solely on extension: classify as "Review & Delete"
        if (hasTempExt) {
            evidence.put("confidence", 0.70);
            return new TempFileEvaluation(
                    true,
                    0.70,
                    String.format("Legacy backup/temp extension (.%s) requiring user review", ext),
                    "Review & Delete",
                    "medium",
                    evidence
            );
        }

        // Case 5: Located in temporary directory without special extension
        if (inTempDir && !isRecentlyModified) {
            evidence.put("confidence", 0.65);
            return new TempFileEvaluation(
                    true,
                    0.65,
                    "File residing in designated temporary folder",
                    "Review & Delete",
                    "medium",
                    evidence
            );
        }

        evidence.put("confidence", 0.0);
        return new TempFileEvaluation(false, 0.0, "Standard non-temporary file", "None", "low", evidence);
    }

    private boolean matchesTempDirectory(String path) {
        if (path == null || path.isBlank()) return false;
        for (String pattern : TEMP_DIR_PATTERNS) {
            if (path.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
