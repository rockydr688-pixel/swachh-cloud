package com.cloudclean.dto;

public class ScanRequest {

    private String target = "Local Computer";
    private String storageType = "local";
    private boolean checkDuplicates = true;
    private boolean checkBlurry = true;
    private boolean checkLargeFiles = true;
    private boolean checkInactive = true;
    private long largeFileSizeThresholdMB = 500;
    private double blurThreshold = 0.85;
    private int inactiveDaysThreshold = 180;

    public ScanRequest() {}

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

    public boolean isCheckDuplicates() {
        return checkDuplicates;
    }

    public void setCheckDuplicates(boolean checkDuplicates) {
        this.checkDuplicates = checkDuplicates;
    }

    public boolean isCheckBlurry() {
        return checkBlurry;
    }

    public void setCheckBlurry(boolean checkBlurry) {
        this.checkBlurry = checkBlurry;
    }

    public boolean isCheckLargeFiles() {
        return checkLargeFiles;
    }

    public void setCheckLargeFiles(boolean checkLargeFiles) {
        this.checkLargeFiles = checkLargeFiles;
    }

    public boolean isCheckInactive() {
        return checkInactive;
    }

    public void setCheckInactive(boolean checkInactive) {
        this.checkInactive = checkInactive;
    }

    public long getLargeFileSizeThresholdMB() {
        return largeFileSizeThresholdMB;
    }

    public void setLargeFileSizeThresholdMB(long largeFileSizeThresholdMB) {
        this.largeFileSizeThresholdMB = largeFileSizeThresholdMB;
    }

    public double getBlurThreshold() {
        return blurThreshold;
    }

    public void setBlurThreshold(double blurThreshold) {
        this.blurThreshold = blurThreshold;
    }

    public int getInactiveDaysThreshold() {
        return inactiveDaysThreshold;
    }

    public void setInactiveDaysThreshold(int inactiveDaysThreshold) {
        this.inactiveDaysThreshold = inactiveDaysThreshold;
    }
}
