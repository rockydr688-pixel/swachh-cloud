package com.cloudclean.detection;

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

@Document(collection = "duplicate_groups")
@CompoundIndex(name = "user_hash_idx", def = "{'userId': 1, 'hash': 1}")
public class DuplicateGroup {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String userId;

    @NotBlank
    private String hash;

    private long fileSizeBytes;

    private String originalFileId;

    private List<String> duplicateFileIds = new ArrayList<>();

    private int totalCopies;

    private long recoverableBytes;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public DuplicateGroup() {}

    public DuplicateGroup(String userId, String hash, long fileSizeBytes, String originalFileId) {
        this.userId = userId;
        this.hash = hash;
        this.fileSizeBytes = fileSizeBytes;
        this.originalFileId = originalFileId;
        this.totalCopies = 1;
        this.recoverableBytes = 0L;
    }

    public void addDuplicate(String fileId) {
        if (!this.duplicateFileIds.contains(fileId) && !fileId.equals(this.originalFileId)) {
            this.duplicateFileIds.add(fileId);
            this.totalCopies = 1 + this.duplicateFileIds.size();
            this.recoverableBytes = this.fileSizeBytes * (this.totalCopies - 1);
        }
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

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getOriginalFileId() {
        return originalFileId;
    }

    public void setOriginalFileId(String originalFileId) {
        this.originalFileId = originalFileId;
    }

    public List<String> getDuplicateFileIds() {
        return duplicateFileIds;
    }

    public void setDuplicateFileIds(List<String> duplicateFileIds) {
        this.duplicateFileIds = duplicateFileIds;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public long getRecoverableBytes() {
        return recoverableBytes;
    }

    public void setRecoverableBytes(long recoverableBytes) {
        this.recoverableBytes = recoverableBytes;
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
