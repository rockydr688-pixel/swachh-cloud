package com.cloudclean.scanner;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "scan_jobs")
@CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}")
public class ScanJob {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String userId;

    private String target; // "Local Computer" | "Google Drive"

    private String storageType = "local";

    private String status = "queued"; // "queued" | "in_progress" | "completed" | "cancelled" | "failed"

    private int progressPercent = 0;

    private int totalFilesScanned = 0;

    private int flaggedFilesCount = 0;

    private long potentialSavingsBytes = 0L;

    private String duration;

    private String errorMessage;

    private Instant startTime;

    private Instant completedTime;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public ScanJob() {}

    public ScanJob(String userId, String target, String storageType) {
        this.userId = userId;
        this.target = target;
        this.storageType = storageType;
        this.status = "queued";
        this.startTime = Instant.now();
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

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public int getTotalFilesScanned() {
        return totalFilesScanned;
    }

    public void setTotalFilesScanned(int totalFilesScanned) {
        this.totalFilesScanned = totalFilesScanned;
    }

    public int getFlaggedFilesCount() {
        return flaggedFilesCount;
    }

    public void setFlaggedFilesCount(int flaggedFilesCount) {
        this.flaggedFilesCount = flaggedFilesCount;
    }

    public long getPotentialSavingsBytes() {
        return potentialSavingsBytes;
    }

    public void setPotentialSavingsBytes(long potentialSavingsBytes) {
        this.potentialSavingsBytes = potentialSavingsBytes;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Instant completedTime) {
        this.completedTime = completedTime;
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
