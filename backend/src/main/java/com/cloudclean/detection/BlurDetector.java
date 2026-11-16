package com.cloudclean.detection;

import com.cloudclean.analysis.FileAnalysis;
import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileAnalysisRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.util.ImageBlurDetector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

@Component
public class BlurDetector {

    private static final Logger log = LoggerFactory.getLogger(BlurDetector.class);

    @Value("${app.ai-service.url:http://127.0.0.1:8001}")
    private String aiServiceUrl;

    @Value("${app.ai-service.secret:cloudclean-ai-internal-key-32chars!}")
    private String aiServiceSecret;

    @Value("${app.ai-service.blur-heavy-threshold:100.0}")
    private double heavyThreshold;

    @Value("${app.ai-service.blur-moderate-threshold:250.0}")
    private double moderateThreshold;

    private final FileAnalysisRepository fileAnalysisRepository;
    private final FileItemRepository fileItemRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public BlurDetector(FileAnalysisRepository fileAnalysisRepository,
                        FileItemRepository fileItemRepository,
                        ObjectMapper objectMapper) {
        this.fileAnalysisRepository = fileAnalysisRepository;
        this.fileItemRepository = fileItemRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public record BlurAnalysisReport(
            int imagesAnalyzed,
            int blurryCount,
            int clearCount,
            List<FileAnalysis> analyses
    ) {}

    /**
     * Analyzes all eligible images for a user using the Python microservice,
     * falling back to local Java Laplacian variance if the microservice is unavailable.
     */
    public BlurAnalysisReport analyzeImages(String userId, double customBlurThreshold) {
        List<FileItem> allUserFiles = fileItemRepository.findByUserIdAndIsDeletedFalse(userId);
        List<FileItem> images = allUserFiles.stream()
                .filter(f -> "image".equalsIgnoreCase(f.getFileType()) || (f.getMimeType() != null && f.getMimeType().startsWith("image/")))
                .toList();

        List<FileAnalysis> analyses = new ArrayList<>();
        int blurryCount = 0;
        int clearCount = 0;

        for (FileItem img : images) {
            ImageBlurDetector.ImageAnalysisResult result = performAnalysis(img);

            if (result != null) {
                img.setBlurScore(result.blurScore());
                img.setQualityScore(result.qualityScore());
                if (result.perceptualHash() != null) {
                    img.setPerceptualHash(result.perceptualHash());
                }

                boolean isBlurry = result.isBlurry() || (result.blurScore() < customBlurThreshold);

                if (isBlurry) {
                    blurryCount++;
                    img.setCategory("blurry");
                    img.setRecommendationStatus("Review & Delete");
                    img.setDetectionReason(String.format("Defective blur: %s (Laplacian variance: %.1f, score: %.2f)",
                            result.classification(), result.variance(), result.blurScore()));
                } else {
                    clearCount++;
                    if ("blurry".equals(img.getCategory())) {
                        img.setCategory("clean");
                        img.setRecommendationStatus("None");
                        img.setDetectionReason("Clear image (sharpness variance " + result.variance() + ")");
                    }
                }

                // Store comprehensive analysis metrics
                FileAnalysis analysis = new FileAnalysis(
                        userId,
                        img.getId(),
                        "blur",
                        result.blurScore(),
                        isBlurry,
                        img.getDetectionReason()
                );

                analysis.setRiskCategory(isBlurry ? "medium" : "low");
                analysis.setRecommendedAction(isBlurry ? "Review & Delete" : "Keep");
                analysis.setPerceptualHash(result.perceptualHash());

                Map<String, Object> metrics = new HashMap<>();
                metrics.put("laplacian_variance", result.variance());
                metrics.put("classification", result.classification());
                metrics.put("quality_score", result.qualityScore());
                metrics.put("width", result.width());
                metrics.put("height", result.height());
                metrics.put("megapixels", result.megapixels());
                metrics.put("is_low_resolution", result.isLowResolution());
                metrics.put("mean_luminance", result.meanIntensity());
                metrics.put("is_very_dark", result.isVeryDark());
                metrics.put("is_overexposed", result.isOverexposed());

                analysis.setMetrics(metrics);
                analyses.add(analysis);
            }
        }

        fileItemRepository.saveAll(images);
        fileAnalysisRepository.saveAll(analyses);

        log.info("Blur detection finished for user {}: {} images evaluated, {} classified blurry, {} clear",
                userId, images.size(), blurryCount, clearCount);

        return new BlurAnalysisReport(images.size(), blurryCount, clearCount, analyses);
    }

    /**
     * Tries calling Python AI microservice first; gracefully falls back to local Java processing.
     */
    public ImageBlurDetector.ImageAnalysisResult performAnalysis(FileItem fileItem) {
        String pathStr = fileItem.getStorageLocation();
        if (pathStr == null || pathStr.isBlank()) return null;

        Path localPath = Paths.get(pathStr);
        if (!Files.exists(localPath)) return null;

        // Try Python AI Microservice
        try {
            Map<String, Object> body = Map.of(
                    "file_path", localPath.toAbsolutePath().toString(),
                    "heavy_threshold", heavyThreshold,
                    "moderate_threshold", moderateThreshold
            );
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/analyze/path"))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Key", aiServiceSecret)
                    .timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.path("success").asBoolean()) {
                    JsonNode data = root.path("data");
                    JsonNode blurNode = data.path("blur");
                    JsonNode dimsNode = data.path("dimensions");
                    JsonNode expNode = data.path("exposure");
                    JsonNode pHashNode = data.path("perceptual_hashes");

                    return new ImageBlurDetector.ImageAnalysisResult(
                            blurNode.path("laplacian_variance").asDouble(150.0),
                            blurNode.path("blur_score").asDouble(0.5),
                            blurNode.path("classification").asText("clear"),
                            blurNode.path("is_blurry").asBoolean(false),
                            dimsNode.path("width").asInt(800),
                            dimsNode.path("height").asInt(600),
                            dimsNode.path("megapixels").asDouble(0.48),
                            data.path("resolution").path("is_low_resolution").asBoolean(false),
                            expNode.path("mean_intensity").asDouble(128.0),
                            expNode.path("is_very_dark").asBoolean(false),
                            expNode.path("is_overexposed").asBoolean(false),
                            data.path("quality_score").asDouble(75.0),
                            pHashNode.path("dhash").asText(null)
                    );
                }
            }
        } catch (Exception e) {
            log.debug("Python AI microservice offline or unreachable ({}), using resilient local Java fallback", e.getMessage());
        }

        // Resilient Local Java Fallback
        return ImageBlurDetector.analyzeImage(localPath);
    }
}
