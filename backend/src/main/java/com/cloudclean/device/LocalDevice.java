package com.cloudclean.device;

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

/**
 * MongoDB Entity representing an authorized local computer or agent workstation.
 */
@Document(collection = "local_devices")
@CompoundIndex(name = "user_device_idx", def = "{'userId': 1, 'deviceName': 1}")
@CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}")
public class LocalDevice {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String userId;

    @NotBlank
    private String deviceName;

    private String osName;

    private String osVersion;

    private String hostname;

    private String agentVersion = "1.0.0";

    private List<String> approvedFolders = new ArrayList<>();

    private String status = "ONLINE"; // "ONLINE" | "OFFLINE" | "SCANNING" | "DISCONNECTED"

    private Instant lastHeartbeat = Instant.now();

    private Instant lastScanTime;

    private int totalScansCompleted = 0;

    private long totalBytesIndexed = 0L;

    @CreatedDate
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    public LocalDevice() {}

    public LocalDevice(String userId, String deviceName, String osName, String hostname) {
        this.userId = userId;
        this.deviceName = deviceName;
        this.osName = osName;
        this.hostname = hostname;
        this.status = "ONLINE";
        this.lastHeartbeat = Instant.now();
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

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public List<String> getApprovedFolders() {
        return approvedFolders;
    }

    public void setApprovedFolders(List<String> approvedFolders) {
        this.approvedFolders = approvedFolders != null ? approvedFolders : new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public Instant getLastScanTime() {
        return lastScanTime;
    }

    public void setLastScanTime(Instant lastScanTime) {
        this.lastScanTime = lastScanTime;
    }

    public int getTotalScansCompleted() {
        return totalScansCompleted;
    }

    public void setTotalScansCompleted(int totalScansCompleted) {
        this.totalScansCompleted = totalScansCompleted;
    }

    public long getTotalBytesIndexed() {
        return totalBytesIndexed;
    }

    public void setTotalBytesIndexed(long totalBytesIndexed) {
        this.totalBytesIndexed = totalBytesIndexed;
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
