package com.cloudclean.storage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "storage_connections")
@CompoundIndex(name = "user_type_idx", def = "{'userId': 1, 'type': 1}")
public class StorageConnection {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String userId;

    @NotBlank
    private String name;

    @NotBlank
    private String type; // "local" | "google_drive"

    private String status = "disconnected"; // "connected" | "disconnected" | "syncing" | "error"

    private String description;

    private List<String> monitoredPaths = new ArrayList<>();

    // Encrypted OAuth tokens (AES encrypted, never exposed plain text)
    private String encryptedAccessToken;
    private String encryptedRefreshToken;
    private Instant tokenExpiresAt;

    private String accountEmail;
    private String accountName;

    private long totalCapacityBytes = 1099511627776L; // default 1 TB
    private long usedBytes = 0L;

    private Instant lastScanTime;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public StorageConnection() {}

    public StorageConnection(String userId, String name, String type) {
        this.userId = userId;
        this.name = name;
        this.type = type;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getMonitoredPaths() {
        return monitoredPaths;
    }

    public void setMonitoredPaths(List<String> monitoredPaths) {
        this.monitoredPaths = monitoredPaths;
    }

    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }

    public void setEncryptedAccessToken(String encryptedAccessToken) {
        this.encryptedAccessToken = encryptedAccessToken;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public void setEncryptedRefreshToken(String encryptedRefreshToken) {
        this.encryptedRefreshToken = encryptedRefreshToken;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public long getTotalCapacityBytes() {
        return totalCapacityBytes;
    }

    public void setTotalCapacityBytes(long totalCapacityBytes) {
        this.totalCapacityBytes = totalCapacityBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(long usedBytes) {
        this.usedBytes = usedBytes;
    }

    public Instant getLastScanTime() {
        return lastScanTime;
    }

    public void setLastScanTime(Instant lastScanTime) {
        this.lastScanTime = lastScanTime;
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
