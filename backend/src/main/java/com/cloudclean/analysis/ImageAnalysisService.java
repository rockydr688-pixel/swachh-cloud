package com.cloudclean.analysis;

import com.cloudclean.detection.BlurDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ImageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ImageAnalysisService.class);

    private final BlurDetector blurDetector;

    public ImageAnalysisService(BlurDetector blurDetector) {
        this.blurDetector = blurDetector;
    }

    public void analyzeImagesForUser(String userId, double blurThreshold) {
        blurDetector.analyzeImages(userId, blurThreshold);
    }
}
