package com.cloudclean.cleanup;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "cleanup_history")
@CompoundIndex(name = "user_timestamp_idx", def = "{'userId': 1, 'timestamp': -1}")
public class CleanupHistory {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String userId;

    @NotBlank
    private String action;

    private String targetCategory;

    private int filesAffected = 0;

    private long freedSpaceBytes = 0L;

    private String status = "success"; // "success" | "partial" | "failed"

    private String executedBy;

    private String method = "Safe Trash / Soft Delete";

    private Instant timestamp;

    private List<String> cleanedFileIds = new ArrayList<>();

    private int failedCount = 0;

    private List<String> failedFileIds = new ArrayList<>();

    private List<String> errorMessages = new ArrayList<>();

    private String errorInformation;

    private String storageProvider = "LOCAL";

    private String originalLocationReference;

    private String recoveryStatus = "AVAILABLE"; // "AVAILABLE" | "RESTORED" | "UNAVAILABLE"

    private boolean restorationSupported = true;

    private boolean isRestored = false;

    private Instant restoredAt;

    private List<Map<String, Object>> cleanedFilesSnapshot = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    public CleanupHistory() {
        this.timestamp = Instant.now();
    }

    public CleanupHistory(String userId, String action, String targetCategory, int filesAffected, long freedSpaceBytes, String executedBy, String method) {
        this.userId = userId;
        this.action = action;
        this.targetCategory = targetCategory;
        this.filesAffected = filesAffected;
        this.freedSpaceBytes = freedSpaceBytes;
        this.executedBy = executedBy;
        this.method = method;
        this.status = "success";
        this.timestamp = Instant.now();
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetCategory() {
        return targetCategory;
    }

    public void setTargetCategory(String targetCategory) {
        this.targetCategory = targetCategory;
    }

    public int getFilesAffected() {
        return filesAffected;
    }

    public void setFilesAffected(int filesAffected) {
        this.filesAffected = filesAffected;
    }

    public long getFreedSpaceBytes() {
        return freedSpaceBytes;
    }

    public void setFreedSpaceBytes(long freedSpaceBytes) {
        this.freedSpaceBytes = freedSpaceBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getCleanedFileIds() {
        return cleanedFileIds;
    }

    public void setCleanedFileIds(List<String> cleanedFileIds) {
        this.cleanedFileIds = cleanedFileIds;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<String> getFailedFileIds() {
        return failedFileIds;
    }

    public void setFailedFileIds(List<String> failedFileIds) {
        this.failedFileIds = failedFileIds;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public boolean isRestorationSupported() {
        return restorationSupported;
    }

    public void setRestorationSupported(boolean restorationSupported) {
        this.restorationSupported = restorationSupported;
    }

    public boolean isRestored() {
        return isRestored;
    }

    public void setRestored(boolean restored) {
        isRestored = restored;
    }

    public Instant getRestoredAt() {
        return restoredAt;
    }

    public void setRestoredAt(Instant restoredAt) {
        this.restoredAt = restoredAt;
    }

    public List<Map<String, Object>> getCleanedFilesSnapshot() {
        return cleanedFilesSnapshot;
    }

    public void setCleanedFilesSnapshot(List<Map<String, Object>> cleanedFilesSnapshot) {
        this.cleanedFilesSnapshot = cleanedFilesSnapshot;
    }

    public String getErrorInformation() {
        if (errorInformation != null) return errorInformation;
        if (errorMessages != null && !errorMessages.isEmpty()) return String.join("; ", errorMessages);
        return null;
    }

    public void setErrorInformation(String errorInformation) {
        this.errorInformation = errorInformation;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public void setStorageProvider(String storageProvider) {
        this.storageProvider = storageProvider;
    }

    public String getOriginalLocationReference() {
        return originalLocationReference;
    }

    public void setOriginalLocationReference(String originalLocationReference) {
        this.originalLocationReference = originalLocationReference;
    }

    public String getRecoveryStatus() {
        if (isRestored) return "RESTORED";
        if (!restorationSupported) return "UNAVAILABLE";
        return recoveryStatus != null ? recoveryStatus : "AVAILABLE";
    }

    public void setRecoveryStatus(String recoveryStatus) {
        this.recoveryStatus = recoveryStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

