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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
