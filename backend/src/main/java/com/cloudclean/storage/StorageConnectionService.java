package com.cloudclean.storage;

import com.cloudclean.dto.GoogleConnectRequest;
import com.cloudclean.dto.StorageConnectionRequest;
import com.cloudclean.dto.StorageConnectionResponse;
import com.cloudclean.exception.BadRequestException;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.repository.StorageConnectionRepository;
import com.cloudclean.util.EncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StorageConnectionService {

    private static final Logger log = LoggerFactory.getLogger(StorageConnectionService.class);

    private final StorageConnectionRepository connectionRepository;
    private final EncryptionUtils encryptionUtils;

    public StorageConnectionService(StorageConnectionRepository connectionRepository,
                                    EncryptionUtils encryptionUtils) {
        this.connectionRepository = connectionRepository;
        this.encryptionUtils = encryptionUtils;
    }

    public List<StorageConnectionResponse> getUserConnections(String userId) {
        List<StorageConnection> connections = connectionRepository.findByUserId(userId);
        if (connections.isEmpty()) {
            // Seed standard initial connection cards for user
            StorageConnection local = new StorageConnection(userId, "Local Computer", "local");
            local.setStatus("connected");
            local.setDescription("Monitored via local-agent daemon IPC. Folder trees indexed with zero cloud exfiltration.");
            local.setMonitoredPaths(List.of("C:/Users/rocky/Downloads", "C:/Users/rocky/Videos/Captures", "D:/Projects/Archive"));
            local.setTotalCapacityBytes(1099511627776L);
            local.setUsedBytes(520000000000L);
            local.setLastScanTime(Instant.now().minusSeconds(3600));

            StorageConnection gdrive = new StorageConnection(userId, "Google Drive", "google_drive");
            gdrive.setStatus("disconnected");
            gdrive.setDescription("Enterprise cloud drive optimization with OAuth 2.0 access.");
            gdrive.setTotalCapacityBytes(1099511627776L);
            gdrive.setUsedBytes(0L);

            connections = connectionRepository.saveAll(List.of(local, gdrive));
        }

        return connections.stream()
                .map(StorageConnectionResponse::new)
                .collect(Collectors.toList());
    }

    public StorageConnectionResponse registerLocalConnection(String userId, StorageConnectionRequest request) {
        StorageConnection connection = connectionRepository.findByUserIdAndType(userId, "local")
                .orElse(new StorageConnection(userId, request.getName(), "local"));

        connection.setName(request.getName());
        connection.setStatus("connected");
        if (request.getPaths() != null && !request.getPaths().isEmpty()) {
            List<String> current = new ArrayList<>(connection.getMonitoredPaths());
            for (String p : request.getPaths()) {
                if (!current.contains(p)) {
                    current.add(p);
                }
            }
            connection.setMonitoredPaths(current);
        }

        StorageConnection saved = connectionRepository.save(connection);
        log.info("Registered local storage paths for user: {}", userId);
        return new StorageConnectionResponse(saved);
    }

    public StorageConnectionResponse connectGoogleDrive(String userId, GoogleConnectRequest request) {
        if (request.getAuthCode() == null || request.getAuthCode().isBlank()) {
            throw new BadRequestException("OAuth authorization code cannot be empty");
        }

        // Encrypt the authorization token using AES
        String encryptedToken = encryptionUtils.encrypt(request.getAuthCode());

        StorageConnection connection = connectionRepository.findByUserIdAndType(userId, "google_drive")
                .orElse(new StorageConnection(userId, "Google Drive", "google_drive"));

        connection.setEncryptedAccessToken(encryptedToken);
        connection.setStatus("connected");
        connection.setAccountEmail("user@workspace.google.com");
        connection.setAccountName("Google Workspace Drive");
        connection.setTokenExpiresAt(Instant.now().plusSeconds(3600 * 24 * 30)); // 30 days
        connection.setLastScanTime(Instant.now());

        StorageConnection saved = connectionRepository.save(connection);
        log.info("Encrypted and stored Google Drive OAuth connection for user: {}", userId);
        return new StorageConnectionResponse(saved);
    }

    public void disconnectConnection(String userId, String connectionId) {
        StorageConnection connection = connectionRepository.findByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Storage connection not found"));

        connection.setStatus("disconnected");
        connection.setEncryptedAccessToken(null);
        connection.setEncryptedRefreshToken(null);
        connection.setAccountEmail(null);
        connection.setAccountName(null);
        connectionRepository.save(connection);
    }
}
