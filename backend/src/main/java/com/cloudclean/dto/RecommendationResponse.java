package com.cloudclean.dto;

import com.cloudclean.recommendation.Recommendation;

import java.time.Instant;
import java.util.List;

public class RecommendationResponse {

    private String id;
    private String title;
    private String category;
    private String type;
    private String priority;
    private String safetyLevel;
    private int safetyScore;
    private long potentialSavingsBytes;
    private int filesCount;
    private String description;
    private String suggestedAction;
    private String storageLocation;
    private String status;
    private List<String> targetFileIds;
    private Instant createdAt;

    public RecommendationResponse() {}

    public RecommendationResponse(Recommendation rec) {
        if (rec != null) {
            this.id = rec.getId();
            this.title = rec.getTitle();
            this.category = rec.getCategory();
            this.type = rec.getType();
            this.priority = rec.getPriority();
            this.safetyLevel = rec.getSafetyLevel();
            this.safetyScore = rec.getSafetyScore();
            this.potentialSavingsBytes = rec.getPotentialSavingsBytes();
            this.filesCount = rec.getFilesCount();
            this.description = rec.getDescription();
            this.suggestedAction = rec.getSuggestedAction();
            this.storageLocation = rec.getStorageLocation();
            this.status = rec.getStatus();
            this.targetFileIds = rec.getTargetFileIds();
            this.createdAt = rec.getCreatedAt();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSafetyLevel() {
        return safetyLevel;
    }

    public void setSafetyLevel(String safetyLevel) {
        this.safetyLevel = safetyLevel;
    }

    public int getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(int safetyScore) {
        this.safetyScore = safetyScore;
    }

    public long getPotentialSavingsBytes() {
        return potentialSavingsBytes;
    }

    public void setPotentialSavingsBytes(long potentialSavingsBytes) {
        this.potentialSavingsBytes = potentialSavingsBytes;
    }

    public int getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getTargetFileIds() {
        return targetFileIds;
    }

    public void setTargetFileIds(List<String> targetFileIds) {
        this.targetFileIds = targetFileIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
