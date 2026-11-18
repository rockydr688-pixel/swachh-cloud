package com.cloudclean.user;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB Entity storing persistent user configurations and threshold preferences.
 */
@Document(collection = "user_preferences")
public class UserPreferences {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank
    private String userId;

    private String autoScanFrequency = "WEEKLY"; // "DAILY" | "WEEKLY" | "MONTHLY" | "MANUAL"

    private int inactivityDaysThreshold = 90; // 30, 90, 180, 365

    private long largeFileThresholdMb = 100L; // 50, 100, 250, 500, 1000

    private double blurVarianceThreshold = 100.0;

    private List<String> protectedExtensions = new ArrayList<>(List.of(
            ".key", ".pem", ".kdbx", ".env", ".crt", ".keystore"
    ));

    private boolean emailAlertsEnabled = true;

    private boolean inAppAlertsEnabled = true;

    private String theme = "system"; // "dark" | "light" | "system"

    private int quotaWarningPercent = 85;

    @CreatedDate
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    public UserPreferences() {}

    public UserPreferences(String userId) {
        this.userId = userId;
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

    public String getAutoScanFrequency() {
        return autoScanFrequency;
    }

    public void setAutoScanFrequency(String autoScanFrequency) {
        this.autoScanFrequency = autoScanFrequency;
    }

    public int getInactivityDaysThreshold() {
        return inactivityDaysThreshold;
    }

    public void setInactivityDaysThreshold(int inactivityDaysThreshold) {
        this.inactivityDaysThreshold = inactivityDaysThreshold;
    }

    public long getLargeFileThresholdMb() {
        return largeFileThresholdMb;
    }

    public void setLargeFileThresholdMb(long largeFileThresholdMb) {
        this.largeFileThresholdMb = largeFileThresholdMb;
    }

    public double getBlurVarianceThreshold() {
        return blurVarianceThreshold;
    }

    public void setBlurVarianceThreshold(double blurVarianceThreshold) {
        this.blurVarianceThreshold = blurVarianceThreshold;
    }

    public List<String> getProtectedExtensions() {
        return protectedExtensions;
    }

    public void setProtectedExtensions(List<String> protectedExtensions) {
        this.protectedExtensions = protectedExtensions != null ? protectedExtensions : new ArrayList<>();
    }

    public boolean isEmailAlertsEnabled() {
        return emailAlertsEnabled;
    }

    public void setEmailAlertsEnabled(boolean emailAlertsEnabled) {
        this.emailAlertsEnabled = emailAlertsEnabled;
    }

    public boolean isInAppAlertsEnabled() {
        return inAppAlertsEnabled;
    }

    public void setInAppAlertsEnabled(boolean inAppAlertsEnabled) {
        this.inAppAlertsEnabled = inAppAlertsEnabled;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public int getQuotaWarningPercent() {
        return quotaWarningPercent;
    }

    public void setQuotaWarningPercent(int quotaWarningPercent) {
        this.quotaWarningPercent = quotaWarningPercent;
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
