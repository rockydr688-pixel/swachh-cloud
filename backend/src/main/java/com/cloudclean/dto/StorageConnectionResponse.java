package com.cloudclean.dto;

import com.cloudclean.storage.StorageConnection;

import java.time.Instant;
import java.util.List;

public class StorageConnectionResponse {

    private String id;
    private String name;
    private String type;
    private String status;
    private String description;
    private List<String> monitoredPaths;
    private String accountEmail;
    private String accountName;
    private long totalCapacityBytes;
    private long usedBytes;
    private Instant lastScanTime;
    private Instant createdAt;

    public StorageConnectionResponse() {}

    public StorageConnectionResponse(StorageConnection conn) {
        if (conn != null) {
            this.id = conn.getId();
            this.name = conn.getName();
            this.type = conn.getType();
            this.status = conn.getStatus();
            this.description = conn.getDescription();
            this.monitoredPaths = conn.getMonitoredPaths();
            this.accountEmail = conn.getAccountEmail();
            this.accountName = conn.getAccountName();
            this.totalCapacityBytes = conn.getTotalCapacityBytes();
            this.usedBytes = conn.getUsedBytes();
            this.lastScanTime = conn.getLastScanTime();
            this.createdAt = conn.getCreatedAt();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
