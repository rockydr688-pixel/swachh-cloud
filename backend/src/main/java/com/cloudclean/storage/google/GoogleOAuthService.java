package com.cloudclean.storage.google;

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
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);

    private static final String GOOGLE_AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";

    // Minimum necessary OAuth scopes requested
    public static final List<String> OAUTH_SCOPES = List.of(
            "https://www.googleapis.com/auth/drive.metadata.readonly",
            "https://www.googleapis.com/auth/drive.file",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile"
    );

    @Value("${app.google.client-id:}")
    private String clientId;

    @Value("${app.google.client-secret:}")
    private String clientSecret;

    @Value("${app.google.redirect-uri:http://localhost:3000/oauth/callback}")
    private String defaultRedirectUri;

    private final EncryptionUtils encryptionUtils;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GoogleOAuthService(EncryptionUtils encryptionUtils, ObjectMapper objectMapper) {
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

    /**
     * Builds the Google OAuth 2.0 authorization consent screen URL.
     */
    public String buildAuthorizationUrl(String state, String redirectUri) {
        String effectiveRedirect = (redirectUri != null && !redirectUri.isBlank()) ? redirectUri : defaultRedirectUri;
        String scopesJoined = String.join(" ", OAUTH_SCOPES);

        return GOOGLE_AUTH_ENDPOINT +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(effectiveRedirect, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode(scopesJoined, StandardCharsets.UTF_8) +
                "&access_type=offline" +
                "&prompt=consent" +
                "&include_granted_scopes=true" +
                (state != null ? "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) : "");
    }

    /**
     * Exchanges an authorization code for access and refresh tokens.
     */
    public OAuthTokenResult exchangeCode(String authorizationCode, String redirectUri) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new BadRequestException("Authorization code cannot be empty");
        }

        String effectiveRedirect = (redirectUri != null && !redirectUri.isBlank()) ? redirectUri : defaultRedirectUri;

        try {
            String formBody = "code=" + URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8) +
                    "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(effectiveRedirect, StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TOKEN_ENDPOINT))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Google OAuth token exchange failed with status {}: {}", response.statusCode(), response.body());
                throw new BadRequestException("Google token exchange failed: " + response.body());
            }

            JsonNode node = objectMapper.readTree(response.body());
            String accessToken = node.path("access_token").asText();
            String refreshToken = node.has("refresh_token") ? node.path("refresh_token").asText() : null;
            long expiresIn = node.path("expires_in").asLong(3600);
            Instant expiresAt = Instant.now().plusSeconds(expiresIn);

            // Fetch user info with the new access token
            UserInfo userInfo = fetchUserInfo(accessToken);

            return new OAuthTokenResult(accessToken, refreshToken, expiresAt, userInfo.email(), userInfo.name());
        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            log.error("Error communicating with Google OAuth endpoint", e);
            throw new BadRequestException("OAuth exchange error: " + e.getMessage());
        }
    }

    /**
     * Automatically refreshes an expired access token using the stored encrypted refresh token.
     */
    public String refreshAccessToken(StorageConnection connection) {
        if (connection == null || connection.getEncryptedRefreshToken() == null) {
            throw new BadRequestException("No refresh token available to refresh Google Drive connection");
        }

        String rawRefreshToken = encryptionUtils.decrypt(connection.getEncryptedRefreshToken());

        try {
            String formBody = "refresh_token=" + URLEncoder.encode(rawRefreshToken, StandardCharsets.UTF_8) +
                    "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&grant_type=refresh_token";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TOKEN_ENDPOINT))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Failed to refresh Google OAuth token, status {}: {}", response.statusCode(), response.body());
                connection.setStatus("error");
                throw new BadRequestException("Google token refresh failed. Please reconnect Google Drive.");
            }

            JsonNode node = objectMapper.readTree(response.body());
            String newAccessToken = node.path("access_token").asText();
            long expiresIn = node.path("expires_in").asLong(3600);

            // Encrypt and update connection
            connection.setEncryptedAccessToken(encryptionUtils.encrypt(newAccessToken));
            connection.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
            connection.setStatus("connected");

            return newAccessToken;
        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            log.error("Error refreshing Google Drive token", e);
            throw new BadRequestException("Error refreshing Google Drive token: " + e.getMessage());
        }
    }

    private record UserInfo(String email, String name) {}

    private UserInfo fetchUserInfo(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_USERINFO_ENDPOINT))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                String email = node.path("email").asText("user@workspace.google.com");
                String name = node.path("name").asText("Google Drive User");
                return new UserInfo(email, name);
            }
        } catch (Exception e) {
            log.warn("Could not fetch Google user profile: {}", e.getMessage());
        }
        return new UserInfo("user@workspace.google.com", "Google User");
    }

    public String getClientId() {
        return clientId;
    }
}
