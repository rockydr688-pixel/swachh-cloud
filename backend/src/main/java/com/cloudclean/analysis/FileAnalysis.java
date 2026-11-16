package com.cloudclean.analysis;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "file_analyses")
@CompoundIndex(name = "user_file_idx", def = "{'userId': 1, 'fileId': 1}")
public class FileAnalysis {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String userId;

    @Indexed
    @NotBlank
    private String fileId;

    private String analysisType; // "duplicate" | "blur" | "large" | "inactivity" | "temporary" | "junk_score"

    private double score; // blur score, quality score, or confidence

    private Integer junkScore; // 0 - 100

    private String riskCategory; // "low" | "medium" | "high"

    private String recommendedAction; // "Safe to Delete" | "Review & Delete" | "Recommended for Archival" | "Keep Original"

    private boolean isFlagged;

    private String flaggedReason;

    private String perceptualHash;

    private Map<String, Object> metrics = new HashMap<>();

    @CreatedDate
    private Instant createdAt;

    public FileAnalysis() {}

    public FileAnalysis(String userId, String fileId, String analysisType, double score, boolean isFlagged, String flaggedReason) {
        this.userId = userId;
        this.fileId = fileId;
        this.analysisType = analysisType;
        this.score = score;
        this.isFlagged = isFlagged;
        this.flaggedReason = flaggedReason;
    }

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

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Integer getJunkScore() {
        return junkScore;
    }

    public void setJunkScore(Integer junkScore) {
        this.junkScore = junkScore;
    }

    public String getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(String riskCategory) {
        this.riskCategory = riskCategory;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getPerceptualHash() {
        return perceptualHash;
    }

    public void setPerceptualHash(String perceptualHash) {
        this.perceptualHash = perceptualHash;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    public void setFlagged(boolean flagged) {
        isFlagged = flagged;
    }

    public String getFlaggedReason() {
        return flaggedReason;
    }

    public void setFlaggedReason(String flaggedReason) {
        this.flaggedReason = flaggedReason;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

