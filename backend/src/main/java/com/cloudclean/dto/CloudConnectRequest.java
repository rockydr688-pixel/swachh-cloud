package com.cloudclean.dto;

import jakarta.validation.constraints.NotBlank;

public class CloudConnectRequest {

    private String provider; // "google_drive" | "onedrive" | "dropbox"

    @NotBlank(message = "Authorization code cannot be empty")
    private String authCode;

    private String redirectUri;

    private String accountName;

    public CloudConnectRequest() {}

    public CloudConnectRequest(String provider, String authCode, String redirectUri) {
        this.provider = provider;
        this.authCode = authCode;
        this.redirectUri = redirectUri;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
}
