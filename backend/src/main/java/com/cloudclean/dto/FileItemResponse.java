package com.cloudclean.dto;

import com.cloudclean.file.FileItem;

import java.time.Instant;
import java.util.Map;

public class FileItemResponse {

    private String id;
    private String name;
    private String extension;
    private String fileType;
    private String category;
    private long sizeBytes;
    private String storageLocation;
    private String storageType;
    private Instant lastModified;
    private Instant lastAccessed;
    private String detectionReason;
    private String recommendationStatus;
    private String hash;
    private Double blurScore;
    private String previewUrl;
    private Map<String, Object> metadata;
    private Instant createdAt;

    public FileItemResponse() {}

    public FileItemResponse(FileItem item) {
        if (item != null) {
            this.id = item.getId();
            this.name = item.getName();
            this.extension = item.getExtension();
            this.fileType = item.getFileType();
            this.category = item.getCategory();
            this.sizeBytes = item.getSizeBytes();
            this.storageLocation = sanitizePath(item.getStorageLocation());
            this.storageType = item.getStorageType();
            this.lastModified = item.getLastModified();
            this.lastAccessed = item.getLastAccessed();
            this.detectionReason = item.getDetectionReason();
            this.recommendationStatus = item.getRecommendationStatus();
            this.hash = item.getHash();
            this.blurScore = item.getBlurScore();
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
