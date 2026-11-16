package com.cloudclean.storage.provider;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Common file metadata representation across local host storage and Google Drive.
 * Complies with strict non-equivalence between modifiedTime and lastAccessedTime.
 */
public class ProviderFileMetadata {

    private String providerFileId;
    private String name;
    private String mimeType;
    private String extension;
    private long sizeBytes;
    private Instant createdTime;
    private Instant modifiedTime;
    private Instant lastAccessedTime;
    private boolean lastAccessedReliable = false;
    private String sha256Hash;
    private String md5Checksum;
    private boolean isDirectory = false;
    private boolean isTrashed = false;
    private String storageLocation;
    private String storageType; // "local" | "google_drive"
    private String webViewLink;
    private String thumbnailLink;
    private Map<String, Object> extraProperties = new HashMap<>();

    public ProviderFileMetadata() {}

    public ProviderFileMetadata(String providerFileId, String name, long sizeBytes, String storageType) {
        this.providerFileId = providerFileId;
        this.name = name;
        this.sizeBytes = sizeBytes;
        this.storageType = storageType;
    }

    public String getProviderFileId() {
        return providerFileId;
    }

    public void setProviderFileId(String providerFileId) {
        this.providerFileId = providerFileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Instant createdTime) {
        this.createdTime = createdTime;
    }

    public Instant getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Instant modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public Instant getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void setLastAccessedTime(Instant lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    public boolean isLastAccessedReliable() {
        return lastAccessedReliable;
    }

    public void setLastAccessedReliable(boolean lastAccessedReliable) {
        this.lastAccessedReliable = lastAccessedReliable;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public String getMd5Checksum() {
        return md5Checksum;
    }

    public void setMd5Checksum(String md5Checksum) {
        this.md5Checksum = md5Checksum;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public boolean isTrashed() {
        return isTrashed;
    }

    public void setTrashed(boolean trashed) {
        isTrashed = trashed;
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

    public String getWebViewLink() {
        return webViewLink;
    }

    public void setWebViewLink(String webViewLink) {
        this.webViewLink = webViewLink;
    }

    public String getThumbnailLink() {
        return thumbnailLink;
    }

    public void setThumbnailLink(String thumbnailLink) {
        this.thumbnailLink = thumbnailLink;
    }

    public Map<String, Object> getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Map<String, Object> extraProperties) {
        this.extraProperties = extraProperties;
    }
}
