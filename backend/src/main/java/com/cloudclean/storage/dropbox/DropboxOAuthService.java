package com.cloudclean.storage.dropbox;

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
import java.util.Base64;
import java.util.List;

@Service
public class DropboxOAuthService {

    private static final Logger log = LoggerFactory.getLogger(DropboxOAuthService.class);

    private static final String DROPBOX_AUTH_ENDPOINT = "https://www.dropbox.com/oauth2/authorize";
    private static final String DROPBOX_TOKEN_ENDPOINT = "https://api.dropboxapi.com/oauth2/token";
    private static final String DROPBOX_ACCOUNT_ENDPOINT = "https://api.dropboxapi.com/2/users/get_current_account";

    public static final List<String> OAUTH_SCOPES = List.of(
            "account_info.read",
            "files.metadata.read",
            "files.content.read",
            "files.content.write"
    );

    @Value("${app.dropbox.client-id:}")
    private String clientId;

    @Value("${app.dropbox.client-secret:}")
    private String clientSecret;

    @Value("${app.dropbox.redirect-uri:http://localhost:3000/oauth/callback}")
    private String defaultRedirectUri;

    private final EncryptionUtils encryptionUtils;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DropboxOAuthService(EncryptionUtils encryptionUtils, ObjectMapper objectMapper) {
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

        return DROPBOX_AUTH_ENDPOINT +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&token_access_type=offline" +
                "&redirect_uri=" + URLEncoder.encode(effectiveRedirect, StandardCharsets.UTF_8) +
                (state != null ? "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) : "");
    }

    public OAuthTokenResult exchangeCode(String authorizationCode, String redirectUri) {
        if (!isConfigured()) {
            throw new BadRequestException("Dropbox integration requires DROPBOX_CLIENT_ID and DROPBOX_CLIENT_SECRET. Please register a Dropbox App and add credentials to .env.");
        }
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new BadRequestException("Authorization code cannot be empty");
        }

        String effectiveRedirect = (redirectUri != null && !redirectUri.isBlank()) ? redirectUri : defaultRedirectUri;

        try {
            String formBody = "code=" + URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code" +
                    "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(effectiveRedirect, StandardCharsets.UTF_8);

            String basicAuth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DROPBOX_TOKEN_ENDPOINT))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + basicAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Dropbox OAuth token exchange failed with status {}: {}", response.statusCode(), response.body());
                throw new BadRequestException("Dropbox token exchange failed: " + response.body());
            }

            JsonNode node = objectMapper.readTree(response.body());
            String accessToken = node.path("access_token").asText();
            String refreshToken = node.has("refresh_token") ? node.path("refresh_token").asText() : null;
            long expiresIn = node.path("expires_in").asLong(14400); // default 4 hours
            Instant expiresAt = Instant.now().plusSeconds(expiresIn);

            UserInfo userInfo = fetchUserInfo(accessToken);

            return new OAuthTokenResult(accessToken, refreshToken, expiresAt, userInfo.email(), userInfo.name());
        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            log.error("Error communicating with Dropbox OAuth endpoint", e);
            throw new BadRequestException("Dropbox OAuth error: " + e.getMessage());
        }
    }

    public String refreshAccessToken(StorageConnection connection) {
        if (!isConfigured()) {
            throw new BadRequestException("Dropbox credentials not configured on backend");
        }
        if (connection == null || connection.getEncryptedRefreshToken() == null) {
            throw new BadRequestException("No refresh token available to refresh Dropbox connection");
        }

        String rawRefreshToken = encryptionUtils.decrypt(connection.getEncryptedRefreshToken());

        try {
            String formBody = "grant_type=refresh_token" +
                    "&refresh_token=" + URLEncoder.encode(rawRefreshToken, StandardCharsets.UTF_8) +
                    "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

            String basicAuth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DROPBOX_TOKEN_ENDPOINT))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + basicAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Failed to refresh Dropbox OAuth token, status {}: {}", response.statusCode(), response.body());
                connection.setStatus("error");
                throw new BadRequestException("Dropbox token refresh failed. Please reconnect your Dropbox account.");
            }

            JsonNode node = objectMapper.readTree(response.body());
            String newAccessToken = node.path("access_token").asText();
            long expiresIn = node.path("expires_in").asLong(14400);

            connection.setEncryptedAccessToken(encryptionUtils.encrypt(newAccessToken));
            connection.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
            connection.setStatus("connected");

            return newAccessToken;
        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            log.error("Error refreshing Dropbox token", e);
            throw new BadRequestException("Error refreshing Dropbox token: " + e.getMessage());
        }
    }

    public record UserInfo(String email, String name) {}

    public UserInfo fetchUserInfo(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DROPBOX_ACCOUNT_ENDPOINT))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("null"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                String email = node.path("email").asText("user@dropbox.com");
                String name = node.path("name").path("display_name").asText("Dropbox User");
                return new UserInfo(email, name);
            }
        } catch (Exception e) {
            log.warn("Could not fetch Dropbox user profile: {}", e.getMessage());
        }
        return new UserInfo("user@dropbox.com", "Dropbox User");
    }

    public String getClientId() {
        return clientId;
    }
}
