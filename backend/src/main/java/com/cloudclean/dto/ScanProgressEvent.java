package com.cloudclean.dto;

import java.time.Instant;

public class ScanProgressEvent {

    private String jobId;
    private String status;              // "queued", "in_progress", "completed", "cancelled", "failed"
    private int progressPercent;        // 0 - 100
    private String stage;               // "INITIALIZING", "DISCOVERING_FILES", "HASHING_DUPLICATES", "AI_IMAGE_ANALYSIS", "LARGE_FILE_AUDIT", "INACTIVE_FILE_AUDIT", "JUNK_SCORING", "COMPLETED"
    private String currentProvider;     // "local", "google_drive", "onedrive", "dropbox"
    private String activeDirectory;     // Currently processed directory or item
    private int filesDiscovered;
    private int filesAnalyzed;
    private int duplicatesDetected;
    private int blurryImagesDetected;
    private int largeFilesDetected;
    private int inactiveFilesDetected;
    private int temporaryFilesDetected;
    private int failedFilesCount;
    private long bytesScanned;
    private long potentialSavingsBytes;
    private String duration;
    private String errorMessage;
    private Instant timestamp;

    public ScanProgressEvent() {
        this.timestamp = Instant.now();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
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

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getCurrentProvider() {
        return currentProvider;
    }

    public void setCurrentProvider(String currentProvider) {
        this.currentProvider = currentProvider;
    }

    public String getActiveDirectory() {
        return activeDirectory;
    }

    public void setActiveDirectory(String activeDirectory) {
        this.activeDirectory = activeDirectory;
    }

    public int getFilesDiscovered() {
        return filesDiscovered;
    }

    public void setFilesDiscovered(int filesDiscovered) {
        this.filesDiscovered = filesDiscovered;
    }

    public int getFilesAnalyzed() {
        return filesAnalyzed;
    }

    public void setFilesAnalyzed(int filesAnalyzed) {
        this.filesAnalyzed = filesAnalyzed;
    }

    public int getDuplicatesDetected() {
        return duplicatesDetected;
    }

    public void setDuplicatesDetected(int duplicatesDetected) {
        this.duplicatesDetected = duplicatesDetected;
    }

    public int getBlurryImagesDetected() {
        return blurryImagesDetected;
    }

    public void setBlurryImagesDetected(int blurryImagesDetected) {
        this.blurryImagesDetected = blurryImagesDetected;
    }

    public int getLargeFilesDetected() {
        return largeFilesDetected;
    }

    public void setLargeFilesDetected(int largeFilesDetected) {
        this.largeFilesDetected = largeFilesDetected;
    }

    public int getInactiveFilesDetected() {
        return inactiveFilesDetected;
    }

    public void setInactiveFilesDetected(int inactiveFilesDetected) {
        this.inactiveFilesDetected = inactiveFilesDetected;
    }

    public int getTemporaryFilesDetected() {
        return temporaryFilesDetected;
    }

    public void setTemporaryFilesDetected(int temporaryFilesDetected) {
        this.temporaryFilesDetected = temporaryFilesDetected;
    }

    public int getFailedFilesCount() {
        return failedFilesCount;
    }

    public void setFailedFilesCount(int failedFilesCount) {
        this.failedFilesCount = failedFilesCount;
    }

    public long getBytesScanned() {
        return bytesScanned;
    }

    public void setBytesScanned(long bytesScanned) {
        this.bytesScanned = bytesScanned;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
