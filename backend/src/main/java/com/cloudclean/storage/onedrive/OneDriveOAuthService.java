package com.cloudclean.storage.onedrive;

import com.cloudclean.exception.BadRequestException;
import com.cloudclean.storage.StorageConnection;
import com.cloudclean.util.EncryptionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class OneDriveOAuthService {

    private static final Logger log = LoggerFactory.getLogger(OneDriveOAuthService.class);

    private static final String MS_AUTH_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    private static final String MS_TOKEN_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private static final String MS_GRAPH_ME_ENDPOINT = "https://graph.microsoft.com/v1.0/me";

    public static final List<String> OAUTH_SCOPES = List.of(
            "offline_access",
            "Files.ReadWrite",
            "User.Read"
    );

    @Value("${app.onedrive.client-id:}")
    private String clientId;

    @Value("${app.onedrive.client-secret:}")
    private String clientSecret;

    @Value("${app.onedrive.redirect-uri:http://localhost:3000/oauth/callback}")
    private String defaultRedirectUri;

    private final EncryptionUtils encryptionUtils;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OneDriveOAuthService(EncryptionUtils encryptionUtils, ObjectMapper objectMapper) {
        this.encryptionUtils = encryptionUtils;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public record OAuthTokenResult(
            String accessToken,
            String refreshToken,
            Instant expiresAt,
            String email,
            String name
    ) {}

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && !clientId.startsWith("mock")
                && clientSecret != null && !clientSecret.isBlank() && !clientSecret.startsWith("mock");
    }

    public String buildAuthorizationUrl(String state, String redirectUri) {
        if (!isConfigured()) {
            return null;
        }
        String effectiveRedirect = (redirectUri != null && !redirectUri.isBlank()) ? redirectUri : defaultRedirectUri;
        String scopesJoined = String.join(" ", OAUTH_SCOPES);

        return MS_AUTH_ENDPOINT +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(effectiveRedirect, StandardCharsets.UTF_8) +
                "&response_mode=query" +
                "&scope=" + URLEncoder.encode(scopesJoined, StandardCharsets.UTF_8) +
                (state != null ? "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) : "");
    }

    public OAuthTokenResult exchangeCode(String authorizationCode, String redirectUri) {
        if (!isConfigured()) {
            throw new BadRequestException("Microsoft OneDrive integration requires ONEDRIVE_CLIENT_ID and ONEDRIVE_CLIENT_SECRET. Please register an Azure AD Application and add credentials to .env.");
        }
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new BadRequestException("Authorization code cannot be empty");
        }

        String effectiveRedirect = (redirectUri != null && !redirectUri.isBlank()) ? redirectUri : defaultRedirectUri;

        try {
            String formBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code" +
                    "&code=" + URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(effectiveRedirect, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(String.join(" ", OAUTH_SCOPES), StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MS_TOKEN_ENDPOINT))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Microsoft OneDrive OAuth token exchange failed status {}: {}", response.statusCode(), response.body());
                throw new BadRequestException("OneDrive token exchange failed: " + response.body());
            }

            JsonNode node = objectMapper.readTree(response.body());
            String accessToken = node.path("access_token").asText();
            String refreshToken = node.has("refresh_token") ? node.path("refresh_token").asText() : null;
            long expiresIn = node.path("expires_in").asLong(3600);
            Instant expiresAt = Instant.now().plusSeconds(expiresIn);

            UserInfo userInfo = fetchUserInfo(accessToken);

            return new OAuthTokenResult(accessToken, refreshToken, expiresAt, userInfo.email(), userInfo.name());
        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            log.error("Error communicating with Microsoft OAuth endpoint", e);
            throw new BadRequestException("Microsoft OAuth error: " + e.getMessage());
        }
    }

    public String refreshAccessToken(StorageConnection connection) {
        if (!isConfigured()) {
            throw new BadRequestException("OneDrive credentials not configured on backend");
        }
        if (connection == null || connection.getEncryptedRefreshToken() == null) {
            throw new BadRequestException("No refresh token available to refresh OneDrive connection");
        }

        String rawRefreshToken = encryptionUtils.decrypt(connection.getEncryptedRefreshToken());

        try {
            String formBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&grant_type=refresh_token" +
                    "&refresh_token=" + URLEncoder.encode(rawRefreshToken, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(String.join(" ", OAUTH_SCOPES), StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MS_TOKEN_ENDPOINT))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Failed to refresh OneDrive OAuth token, status {}: {}", response.statusCode(), response.body());
                connection.setStatus("error");
                throw new BadRequestException("OneDrive token refresh failed. Please reconnect your Microsoft OneDrive account.");
            }

            JsonNode node = objectMapper.readTree(response.body());
            String newAccessToken = node.path("access_token").asText();
            String newRefreshToken = node.has("refresh_token") ? node.path("refresh_token").asText() : rawRefreshToken;
            long expiresIn = node.path("expires_in").asLong(3600);

            connection.setEncryptedAccessToken(encryptionUtils.encrypt(newAccessToken));
            connection.setEncryptedRefreshToken(encryptionUtils.encrypt(newRefreshToken));
            connection.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
            connection.setStatus("connected");

            return newAccessToken;
        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            log.error("Error refreshing OneDrive token", e);
            throw new BadRequestException("Error refreshing OneDrive token: " + e.getMessage());
        }
    }

    public record UserInfo(String email, String name) {}

    public UserInfo fetchUserInfo(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MS_GRAPH_ME_ENDPOINT))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                String email = node.has("mail") && !node.path("mail").isNull()
                        ? node.path("mail").asText()
                        : node.path("userPrincipalName").asText("user@onedrive.com");
                String name = node.path("displayName").asText("OneDrive User");
                return new UserInfo(email, name);
            }
        } catch (Exception e) {
            log.warn("Could not fetch Microsoft user profile: {}", e.getMessage());
        }
        return new UserInfo("user@onedrive.com", "OneDrive User");
    }

    public String getClientId() {
        return clientId;
    }
}
