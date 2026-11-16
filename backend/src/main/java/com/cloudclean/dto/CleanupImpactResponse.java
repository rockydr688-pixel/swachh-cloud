package com.cloudclean.dto;

import java.util.List;

public class CleanupImpactResponse {

    private int totalFilesSelected;
    private long estimatedRecoverableBytes;
    private int protectedFilesSkipped;
    private List<FileItemResponse> files;
    private String warningMessage;
    private boolean confirmationRequired = true;

    public CleanupImpactResponse() {}

    public CleanupImpactResponse(int totalFilesSelected, long estimatedRecoverableBytes, int protectedFilesSkipped, List<FileItemResponse> files, String warningMessage) {
        this.totalFilesSelected = totalFilesSelected;
        this.estimatedRecoverableBytes = estimatedRecoverableBytes;
        this.protectedFilesSkipped = protectedFilesSkipped;
        this.files = files;
        this.warningMessage = warningMessage;
        this.confirmationRequired = true;
    }

    public int getTotalFilesSelected() {
        return totalFilesSelected;
    }

    public void setTotalFilesSelected(int totalFilesSelected) {
        this.totalFilesSelected = totalFilesSelected;
    }

    public long getEstimatedRecoverableBytes() {
        return estimatedRecoverableBytes;
    }

    public void setEstimatedRecoverableBytes(long estimatedRecoverableBytes) {
        this.estimatedRecoverableBytes = estimatedRecoverableBytes;
    }

    public int getProtectedFilesSkipped() {
        return protectedFilesSkipped;
    }

    public void setProtectedFilesSkipped(int protectedFilesSkipped) {
        this.protectedFilesSkipped = protectedFilesSkipped;
    }

    public List<FileItemResponse> getFiles() {
        return files;
    }

    public void setFiles(List<FileItemResponse> files) {
        this.files = files;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }
}
