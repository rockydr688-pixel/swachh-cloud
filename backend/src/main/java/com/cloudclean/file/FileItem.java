package com.cloudclean.file;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "files")
@CompoundIndex(name = "user_category_idx", def = "{'userId': 1, 'category': 1}")
@CompoundIndex(name = "user_hash_idx", def = "{'userId': 1, 'hash': 1}")
public class FileItem {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String userId;

    @Indexed
    private String storageConnectionId;

    @Indexed
    private String providerFileId;

    @NotBlank
    private String name;

    private String extension;

    private String mimeType;

    private String fileType; // "image" | "video" | "archive" | "document" | "application" | "data" | "other"

    @Indexed
    private String category = "clean"; // "duplicate" | "blurry" | "large" | "inactive" | "clean"

    private long sizeBytes;

    private String storageLocation; // Masked path / safe storage location

    private String storageType; // "local" | "google_drive"

    private Instant createdTime;

    private Instant lastModified;

    private Instant lastAccessed;

    private boolean lastAccessedReliable = false;

    private String scanStatus = "scanned"; // "scanned" | "flagged" | "failed" | "pending"

    private String errorMessage;

    private String detectionReason;

    private String recommendationStatus = "None"; // "Safe to Delete" | "Review & Delete" | "Recommended for Archival" | "Keep Original"

    @Indexed
    private String hash; // SHA-256

    private String md5Hash;

    private Double blurScore; // Laplacian score 0.0 - 1.0

    private String previewUrl;

    private Integer junkScore = 0; // 0 - 100

    private String riskCategory = "low"; // "low" | "medium" | "high"

    private String perceptualHash; // 64-bit dHash hex

    private boolean isTemporary = false;

    private String inactivityCategory = "active"; // "30_days" | "90_days" | "180_days" | "365_days" | "active"

    private Double qualityScore; // 0.0 - 100.0

    private Map<String, Object> evidence = new HashMap<>();

    private boolean isDeleted = false;

    private boolean isProtected = false;

    private boolean isExcluded = false;

    private Instant deletedAt;

    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public FileItem() {}

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

    public String getStorageConnectionId() {
        return storageConnectionId;
    }

    public void setStorageConnectionId(String storageConnectionId) {
        this.storageConnectionId = storageConnectionId;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
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

    public String getPerceptualHash() {
        return perceptualHash;
    }

    public void setPerceptualHash(String perceptualHash) {
        this.perceptualHash = perceptualHash;
    }

    public boolean isTemporary() {
        return isTemporary;
    }

    public void setTemporary(boolean temporary) {
        isTemporary = temporary;
    }

    public String getInactivityCategory() {
        return inactivityCategory;
    }

    public void setInactivityCategory(String inactivityCategory) {
        this.inactivityCategory = inactivityCategory;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }

    public void setEvidence(Map<String, Object> evidence) {
        this.evidence = evidence;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
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

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

