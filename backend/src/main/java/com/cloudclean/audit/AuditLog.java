package com.cloudclean.audit;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "audit_logs")
@CompoundIndex(name = "user_time_idx", def = "{'userId': 1, 'timestamp': -1}")
public class AuditLog {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String userId;

    @NotBlank
    private String action; // e.g. "CLEANUP_EXECUTE", "CLEANUP_RESTORE", "RECOMMENDATION_ACCEPT", "FILE_PROTECT"

    private String resourceType; // "file", "recommendation", "cleanup", "scan"

    private String resourceId;

    private String details;

    private boolean success = true;

    private String clientIp;

    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    private Instant timestamp = Instant.now();

    public AuditLog() {}

    public AuditLog(String userId, String action, String resourceType, String resourceId, String details, boolean success) {
        this.userId = userId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.details = details;
        this.success = success;
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

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
