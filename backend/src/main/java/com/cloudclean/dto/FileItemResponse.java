package com.cloudclean.dto;

import com.cloudclean.file.FileItem;

import java.time.Instant;
import java.util.Map;

public class FileItemResponse {

    private String id;
    private String providerFileId;
    private String name;
    private String extension;
    private String mimeType;
    private String fileType;
    private String category;
    private long sizeBytes;
    private String storageLocation;
    private String storageType;
    private Instant createdTime;
    private Instant lastModified;
    private Instant lastAccessed;
    private boolean lastAccessedReliable;
    private String scanStatus;
    private String detectionReason;
    private String recommendationStatus;
    private String hash;
    private Double blurScore;
    private Integer junkScore;
    private String riskCategory;
    private Double qualityScore;
    private String inactivityCategory;
    private boolean isTemporary;
    private Map<String, Object> evidence;
    private String previewUrl;
    private Map<String, Object> metadata;
    private Instant createdAt;

    public FileItemResponse() {}

    public FileItemResponse(FileItem item) {
        if (item != null) {
            this.id = item.getId();
            this.providerFileId = item.getProviderFileId();
            this.name = item.getName();
            this.extension = item.getExtension();
            this.mimeType = item.getMimeType();
            this.fileType = item.getFileType();
            this.category = item.getCategory();
            this.sizeBytes = item.getSizeBytes();
            this.storageLocation = sanitizePath(item.getStorageLocation());
            this.storageType = item.getStorageType();
            this.createdTime = item.getCreatedTime();
            this.lastModified = item.getLastModified();
            this.lastAccessed = item.getLastAccessed();
            this.lastAccessedReliable = item.isLastAccessedReliable();
            this.scanStatus = item.getScanStatus();
            this.detectionReason = item.getDetectionReason();
            this.recommendationStatus = item.getRecommendationStatus();
            this.hash = item.getHash();
            this.blurScore = item.getBlurScore();
            this.junkScore = item.getJunkScore();
            this.riskCategory = item.getRiskCategory();
            this.qualityScore = item.getQualityScore();
            this.inactivityCategory = item.getInactivityCategory();
            this.isTemporary = item.isTemporary();
            this.evidence = item.getEvidence();
            this.previewUrl = item.getPreviewUrl();
            this.metadata = item.getMetadata();
            this.createdAt = item.getCreatedAt();
        }
    }

    private String sanitizePath(String path) {
        if (path == null) return "Storage Directory";
        // Sanitize sensitive user root folders
        return path.replaceAll("^[A-Za-z]:/Users/[^/]+/", "User:/")
                   .replaceAll("^/home/[^/]+/", "User:/");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderFileId() {
        return providerFileId;
    }

    public void setProviderFileId(String providerFileId) {
        this.providerFileId = providerFileId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Instant createdTime) {
        this.createdTime = createdTime;
    }

    public boolean isLastAccessedReliable() {
        return lastAccessedReliable;
    }

    public void setLastAccessedReliable(boolean lastAccessedReliable) {
        this.lastAccessedReliable = lastAccessedReliable;
    }

    public String getScanStatus() {
        return scanStatus;
    }

    public void setScanStatus(String scanStatus) {
        this.scanStatus = scanStatus;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public Instant getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(Instant lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public String getDetectionReason() {
        return detectionReason;
    }

    public void setDetectionReason(String detectionReason) {
        this.detectionReason = detectionReason;
    }

    public String getRecommendationStatus() {
        return recommendationStatus;
    }

    public void setRecommendationStatus(String recommendationStatus) {
        this.recommendationStatus = recommendationStatus;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Double getBlurScore() {
        return blurScore;
    }

    public void setBlurScore(Double blurScore) {
        this.blurScore = blurScore;
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

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getInactivityCategory() {
        return inactivityCategory;
    }

    public void setInactivityCategory(String inactivityCategory) {
        this.inactivityCategory = inactivityCategory;
    }

    public boolean isTemporary() {
        return isTemporary;
    }

    public void setTemporary(boolean temporary) {
        isTemporary = temporary;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }

    public void setEvidence(Map<String, Object> evidence) {
        this.evidence = evidence;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

