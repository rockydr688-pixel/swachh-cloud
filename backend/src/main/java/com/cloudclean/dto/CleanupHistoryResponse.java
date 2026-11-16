package com.cloudclean.dto;

import com.cloudclean.cleanup.CleanupHistory;

import java.time.Instant;
import java.util.List;

public class CleanupHistoryResponse {

    private String id;
    private String action;
    private String targetCategory;
    private int filesAffected;
    private long freedSpaceBytes;
    private String status;
    private String executedBy;
    private String method;
    private Instant timestamp;
    private List<String> cleanedFileIds;
    private int failedCount;
    private List<String> failedFileIds;
    private List<String> errorMessages;
    private boolean restorationSupported;
    private boolean isRestored;
    private Instant restoredAt;

    public CleanupHistoryResponse() {}

    public CleanupHistoryResponse(CleanupHistory history) {
        if (history != null) {
            this.id = history.getId();
            this.action = history.getAction();
            this.targetCategory = history.getTargetCategory();
            this.filesAffected = history.getFilesAffected();
            this.freedSpaceBytes = history.getFreedSpaceBytes();
            this.status = history.getStatus();
            this.executedBy = history.getExecutedBy();
            this.method = history.getMethod();
            this.timestamp = history.getTimestamp();
            this.cleanedFileIds = history.getCleanedFileIds();
            this.failedCount = history.getFailedCount();
            this.failedFileIds = history.getFailedFileIds();
            this.errorMessages = history.getErrorMessages();
            this.restorationSupported = history.isRestorationSupported();
            this.isRestored = history.isRestored();
            this.restoredAt = history.getRestoredAt();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}

