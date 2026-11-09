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
}
