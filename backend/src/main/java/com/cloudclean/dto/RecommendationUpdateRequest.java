package com.cloudclean.dto;

import jakarta.validation.constraints.NotBlank;

public class RecommendationUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status; // "applied" | "dismissed" | "pending"

    public RecommendationUpdateRequest() {}

    public RecommendationUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
