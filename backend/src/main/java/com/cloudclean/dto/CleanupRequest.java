package com.cloudclean.dto;

import jakarta.validation.constraints.AssertTrue;
import java.util.List;

public class CleanupRequest {

    private String recommendationId;
    private List<String> fileIds;
    private String action = "Batch Cleanup";
    private String method = "Safe Trash / Soft Delete";

    @AssertTrue(message = "Explicit user confirmation is strictly required to execute cleanup")
    private boolean confirmed;

    public CleanupRequest() {}

    public CleanupRequest(String recommendationId, boolean confirmed) {
        this.recommendationId = recommendationId;
        this.confirmed = confirmed;
    }

    public CleanupRequest(List<String> fileIds, boolean confirmed) {
        this.fileIds = fileIds;
        this.confirmed = confirmed;
    }

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public List<String> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<String> fileIds) {
        this.fileIds = fileIds;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }
}
