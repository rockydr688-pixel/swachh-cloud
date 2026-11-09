package com.cloudclean.detection;

import com.cloudclean.file.FileItem;
import com.cloudclean.repository.DuplicateGroupRepository;
import com.cloudclean.repository.FileItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DuplicateDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateDetectionService.class);

    private final DuplicateGroupRepository duplicateGroupRepository;
    private final FileItemRepository fileItemRepository;

    public DuplicateDetectionService(DuplicateGroupRepository duplicateGroupRepository,
                                     FileItemRepository fileItemRepository) {
        this.duplicateGroupRepository = duplicateGroupRepository;
        this.fileItemRepository = fileItemRepository;
    }

    public List<DuplicateGroup> processDuplicateDetection(String userId) {
        List<FileItem> allFiles = fileItemRepository.findByUserIdAndIsDeletedFalse(userId);

        Map<String, List<FileItem>> hashGroups = new HashMap<>();
        for (FileItem file : allFiles) {
            if (file.getHash() != null && !file.getHash().isBlank()) {
                hashGroups.computeIfAbsent(file.getHash(), k -> new ArrayList<>()).add(file);
            }
        }

        duplicateGroupRepository.deleteByUserId(userId);
        List<DuplicateGroup> duplicateGroups = new ArrayList<>();

        for (Map.Entry<String, List<FileItem>> entry : hashGroups.entrySet()) {
            List<FileItem> groupFiles = entry.getValue();
            if (groupFiles.size() > 1) {
                // First is canonical original, others are redundant duplicates
                FileItem original = groupFiles.get(0);
                DuplicateGroup group = new DuplicateGroup(userId, entry.getKey(), original.getSizeBytes(), original.getId());

                for (int i = 1; i < groupFiles.size(); i++) {
                    FileItem dup = groupFiles.get(i);
                    group.addDuplicate(dup.getId());

                    dup.setCategory("duplicate");
                    dup.setRecommendationStatus("Safe to Delete");
                    dup.setDetectionReason("Exact hash clone of original: " + original.getName());
                }

                original.setCategory("duplicate");
                original.setRecommendationStatus("Keep Original");
                original.setDetectionReason("Canonical master copy");

                duplicateGroups.add(group);
            }
        }

        fileItemRepository.saveAll(allFiles);
        return duplicateGroupRepository.saveAll(duplicateGroups);
    }
}
