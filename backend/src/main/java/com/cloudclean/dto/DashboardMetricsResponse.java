package com.cloudclean.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardMetricsResponse {

    private long totalCapacityBytes;
    private long totalUsedBytes;
    private long recoverableBytes;
    private long totalFilesScanned;
    private long duplicateFilesCount;
    private long duplicateBytes;
    private long blurryImagesCount;
    private long blurryImagesBytes;
    private long largeFilesCount;
    private long largeFilesBytes;
    private long inactiveFilesCount;
    private long inactiveFilesBytes;
    private int healthScore;
    private Instant lastScanTimestamp;

    private List<Map<String, Object>> storageUsageHistory = new ArrayList<>();
    private List<Map<String, Object>> categoryDistribution = new ArrayList<>();
    private List<RecommendationResponse> recentRecommendations = new ArrayList<>();
    private List<ScanJobResponse> recentScans = new ArrayList<>();

    public DashboardMetricsResponse() {}

    public long getTotalCapacityBytes() {
        return totalCapacityBytes;
    }

    public void setTotalCapacityBytes(long totalCapacityBytes) {
        this.totalCapacityBytes = totalCapacityBytes;
    }

    public long getTotalUsedBytes() {
        return totalUsedBytes;
    }

    public void setTotalUsedBytes(long totalUsedBytes) {
        this.totalUsedBytes = totalUsedBytes;
    }

    public long getRecoverableBytes() {
        return recoverableBytes;
    }

    public void setRecoverableBytes(long recoverableBytes) {
        this.recoverableBytes = recoverableBytes;
    }

    public long getTotalFilesScanned() {
        return totalFilesScanned;
    }

    public void setTotalFilesScanned(long totalFilesScanned) {
        this.totalFilesScanned = totalFilesScanned;
    }

    public long getDuplicateFilesCount() {
        return duplicateFilesCount;
    }

    public void setDuplicateFilesCount(long duplicateFilesCount) {
        this.duplicateFilesCount = duplicateFilesCount;
    }

    public long getDuplicateBytes() {
        return duplicateBytes;
    }

    public void setDuplicateBytes(long duplicateBytes) {
        this.duplicateBytes = duplicateBytes;
    }

    public long getBlurryImagesCount() {
        return blurryImagesCount;
    }

    public void setBlurryImagesCount(long blurryImagesCount) {
        this.blurryImagesCount = blurryImagesCount;
    }

    public long getBlurryImagesBytes() {
        return blurryImagesBytes;
    }

    public void setBlurryImagesBytes(long blurryImagesBytes) {
        this.blurryImagesBytes = blurryImagesBytes;
    }

    public long getLargeFilesCount() {
        return largeFilesCount;
    }

    public void setLargeFilesCount(long largeFilesCount) {
        this.largeFilesCount = largeFilesCount;
    }

    public long getLargeFilesBytes() {
        return largeFilesBytes;
    }

    public void setLargeFilesBytes(long largeFilesBytes) {
        this.largeFilesBytes = largeFilesBytes;
    }

    public long getInactiveFilesCount() {
        return inactiveFilesCount;
    }

    public void setInactiveFilesCount(long inactiveFilesCount) {
        this.inactiveFilesCount = inactiveFilesCount;
    }

    public long getInactiveFilesBytes() {
        return inactiveFilesBytes;
    }

    public void setInactiveFilesBytes(long inactiveFilesBytes) {
        this.inactiveFilesBytes = inactiveFilesBytes;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public Instant getLastScanTimestamp() {
        return lastScanTimestamp;
    }

    public void setLastScanTimestamp(Instant lastScanTimestamp) {
        this.lastScanTimestamp = lastScanTimestamp;
    }

    public List<Map<String, Object>> getStorageUsageHistory() {
        return storageUsageHistory;
    }

    public void setStorageUsageHistory(List<Map<String, Object>> storageUsageHistory) {
        this.storageUsageHistory = storageUsageHistory;
    }

    public List<Map<String, Object>> getCategoryDistribution() {
        return categoryDistribution;
    }

    public void setCategoryDistribution(List<Map<String, Object>> categoryDistribution) {
        this.categoryDistribution = categoryDistribution;
    }

    public List<RecommendationResponse> getRecentRecommendations() {
        return recentRecommendations;
    }

    public void setRecentRecommendations(List<RecommendationResponse> recentRecommendations) {
        this.recentRecommendations = recentRecommendations;
    }

    public List<ScanJobResponse> getRecentScans() {
        return recentScans;
    }

    public void setRecentScans(List<ScanJobResponse> recentScans) {
        this.recentScans = recentScans;
    }
}
