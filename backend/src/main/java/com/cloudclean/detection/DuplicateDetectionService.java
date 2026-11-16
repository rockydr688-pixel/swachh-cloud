package com.cloudclean.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DuplicateDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateDetectionService.class);

    private final DuplicateDetector duplicateDetector;

    public DuplicateDetectionService(DuplicateDetector duplicateDetector) {
        this.duplicateDetector = duplicateDetector;
    }

    public List<DuplicateGroup> processDuplicateDetection(String userId) {
        DuplicateDetector.DuplicateDetectionReport report = duplicateDetector.detectDuplicates(userId);
        return report.duplicateGroups();
    }

    public List<DuplicateDetector.SimilarImageGroup> detectVisuallySimilarImages(String userId, int maxHammingDistance) {
        return duplicateDetector.detectVisuallySimilarImages(userId, maxHammingDistance);
    }
}
