package com.cloudclean.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleConnectRequest {

    @NotBlank(message = "Authorization code or token is required")
    private String authCode;

    private String redirectUri;

    public GoogleConnectRequest() {}

    public GoogleConnectRequest(String authCode, String redirectUri) {
        this.authCode = authCode;
        this.redirectUri = redirectUri;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
