package com.cloudclean.analysis;

import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileAnalysisRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.util.ImageBlurDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ImageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ImageAnalysisService.class);

    private final FileAnalysisRepository fileAnalysisRepository;
    private final FileItemRepository fileItemRepository;

    public ImageAnalysisService(FileAnalysisRepository fileAnalysisRepository,
                                FileItemRepository fileItemRepository) {
        this.fileAnalysisRepository = fileAnalysisRepository;
        this.fileItemRepository = fileItemRepository;
    }

    public void analyzeImagesForUser(String userId, double blurThreshold) {
        List<FileItem> images = fileItemRepository.findByUserIdAndCategoryAndIsDeletedFalse(userId, "blurry");
        for (FileItem image : images) {
            Double score = image.getBlurScore();
            if (score == null && image.getStorageLocation() != null) {
                try {
                    Path path = Paths.get(image.getStorageLocation());
                    score = ImageBlurDetector.calculateClarityScore(path);
                } catch (Exception ignored) {}
            }

            if (score != null) {
                image.setBlurScore(score);
                boolean isBlurry = score < blurThreshold;
                if (isBlurry) {
                    image.setCategory("blurry");
                    image.setRecommendationStatus("Review & Delete");
                    image.setDetectionReason(String.format("Blur score %.2f < %.2f clarity threshold (Laplacian edge variance)", score, blurThreshold));
                }

                FileAnalysis analysis = new FileAnalysis(userId, image.getId(), "blur", score, isBlurry, image.getDetectionReason());
                fileAnalysisRepository.save(analysis);
            }
        }
        fileItemRepository.saveAll(images);
    }
}
