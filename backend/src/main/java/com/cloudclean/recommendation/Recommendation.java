package com.cloudclean.recommendation;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "recommendations")
@CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}")
public class Recommendation {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String userId;

    @NotBlank
    private String title;

    private String category; // "Duplicates" | "Large Files" | "Blurry Images" | "Inactive Files"

    private String type; // "duplicate" | "large" | "blurry" | "inactive"

    private String priority = "medium"; // "high" | "medium" | "low"

    private String safetyLevel = "Safe to Delete"; // "Safe to Delete" | "Review Recommended"

    private int safetyScore = 95; // 0 - 100

    private long potentialSavingsBytes = 0L;

    private int filesCount = 0;

    private String description;

    private String suggestedAction;

    private String storageLocation;

    private String status = "pending"; // "pending" | "applied" | "dismissed" | "accepted" | "rejected"

    private List<String> targetFileIds = new ArrayList<>();

    private String fileId;

    private String fileName;

    private String fileType;

    private long fileSize = 0L;

    private String storageSource = "local";

    private int junkScore = 0;

    private List<String> detectionReasons = new ArrayList<>();

    private String recommendedAction = "Review & Delete";

    private String riskLevel = "medium"; // "low" | "medium" | "high"

    private Instant analysisDate = Instant.now();

    private boolean isProtected = false;

    private boolean isExcluded = false;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Recommendation() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

