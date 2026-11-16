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
    private String fileId;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String storageSource;
    private int junkScore;
    private List<String> detectionReasons;
    private String recommendedAction;
    private String riskLevel;
    private Instant analysisDate;
    private boolean isProtected;
    private boolean isExcluded;
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
            this.fileId = rec.getFileId();
            this.fileName = rec.getFileName();
            this.fileType = rec.getFileType();
            this.fileSize = rec.getFileSize();
            this.storageSource = rec.getStorageSource();
            this.junkScore = rec.getJunkScore();
            this.detectionReasons = rec.getDetectionReasons();
            this.recommendedAction = rec.getRecommendedAction();
            this.riskLevel = rec.getRiskLevel();
            this.analysisDate = rec.getAnalysisDate();
            this.isProtected = rec.isProtected();
            this.isExcluded = rec.isExcluded();
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

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStorageSource() {
        return storageSource;
    }

    public void setStorageSource(String storageSource) {
        this.storageSource = storageSource;
    }

    public int getJunkScore() {
        return junkScore;
    }

    public void setJunkScore(int junkScore) {
        this.junkScore = junkScore;
    }

    public List<String> getDetectionReasons() {
        return detectionReasons;
    }

    public void setDetectionReasons(List<String> detectionReasons) {
        this.detectionReasons = detectionReasons;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Instant getAnalysisDate() {
        return analysisDate;
    }

    public void setAnalysisDate(Instant analysisDate) {
        this.analysisDate = analysisDate;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public void setProtected(boolean aProtected) {
        isProtected = aProtected;
    }

    public boolean isExcluded() {
        return isExcluded;
    }

    public void setExcluded(boolean excluded) {
        isExcluded = excluded;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

