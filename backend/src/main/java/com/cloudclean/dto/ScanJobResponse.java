package com.cloudclean.dto;

import com.cloudclean.scanner.ScanJob;

import java.time.Instant;

public class ScanJobResponse {

    private String id;
    private String target;
    private String storageType;
    private String status;
    private int progressPercent;
    private int totalFilesScanned;
    private int flaggedFilesCount;
    private long potentialSavingsBytes;
    private String duration;
    private String errorMessage;
    private Instant startTime;
    private Instant completedTime;
    private Instant createdAt;

    public ScanJobResponse() {}

    public ScanJobResponse(ScanJob job) {
        if (job != null) {
            this.id = job.getId();
            this.target = job.getTarget();
            this.storageType = job.getStorageType();
            this.status = job.getStatus();
            this.progressPercent = job.getProgressPercent();
            this.totalFilesScanned = job.getTotalFilesScanned();
            this.flaggedFilesCount = job.getFlaggedFilesCount();
            this.potentialSavingsBytes = job.getPotentialSavingsBytes();
            this.duration = job.getDuration();
            this.errorMessage = job.getErrorMessage();
            this.startTime = job.getStartTime();
            this.completedTime = job.getCompletedTime();
            this.createdAt = job.getCreatedAt();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
