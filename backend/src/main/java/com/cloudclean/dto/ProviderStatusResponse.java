package com.cloudclean.dto;

import java.util.List;

public class ProviderStatusResponse {

    private String providerKey;     // "google_drive" | "onedrive" | "dropbox" | "local"
    private String displayName;     // "Google Drive", "Microsoft OneDrive", "Dropbox", "Local Computer"
    private boolean configured;     // true if required client ID/secret are set in env
    private boolean supported;      // true (all 4 are fully supported)
    private String authorizationUrl;// OAuth authorization URL if configured
    private String setupInstructions; // Guided instructions if awaiting configuration
    private List<String> scopes;    // OAuth scopes requested

    public ProviderStatusResponse() {}

    public ProviderStatusResponse(String providerKey, String displayName, boolean configured, boolean supported,
                                  String authorizationUrl, String setupInstructions, List<String> scopes) {
        this.providerKey = providerKey;
        this.displayName = displayName;
        this.configured = configured;
        this.supported = supported;
        this.authorizationUrl = authorizationUrl;
        this.setupInstructions = setupInstructions;
        this.scopes = scopes;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public boolean isSupported() {
        return supported;
    }

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public String getSetupInstructions() {
        return setupInstructions;
    }

    public void setSetupInstructions(String setupInstructions) {
        this.setupInstructions = setupInstructions;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}
