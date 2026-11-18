package com.cloudclean.device;

import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.repository.LocalDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LocalDeviceService {

    private static final Logger log = LoggerFactory.getLogger(LocalDeviceService.class);

    private final LocalDeviceRepository localDeviceRepository;

    public LocalDeviceService(LocalDeviceRepository localDeviceRepository) {
        this.localDeviceRepository = localDeviceRepository;
    }

    public List<LocalDevice> getUserDevices(String userId) {
        return localDeviceRepository.findByUserId(userId);
    }

    public LocalDevice registerOrUpdateDevice(String userId, LocalDevice deviceRequest) {
        LocalDevice device = localDeviceRepository.findByUserIdAndHostname(userId, deviceRequest.getHostname())
                .orElse(new LocalDevice());

        device.setUserId(userId);
        device.setDeviceName(deviceRequest.getDeviceName());
        device.setOsName(deviceRequest.getOsName());
        device.setOsVersion(deviceRequest.getOsVersion());
        device.setHostname(deviceRequest.getHostname());
        device.setAgentVersion(deviceRequest.getAgentVersion() != null ? deviceRequest.getAgentVersion() : "1.0.0");
        device.setStatus("ONLINE");
        device.setLastHeartbeat(Instant.now());

        if (deviceRequest.getApprovedFolders() != null && !deviceRequest.getApprovedFolders().isEmpty()) {
            device.setApprovedFolders(deviceRequest.getApprovedFolders());
        }

        LocalDevice saved = localDeviceRepository.save(device);
        log.info("Registered/updated local device [{}] for user: {}", saved.getDeviceName(), userId);
        return saved;
    }

    public LocalDevice heartbeat(String userId, String deviceId) {
        LocalDevice device = localDeviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Local device not found: " + deviceId));

        device.setLastHeartbeat(Instant.now());
        device.setStatus("ONLINE");
        return localDeviceRepository.save(device);
    }

    public void removeDevice(String userId, String deviceId) {
        LocalDevice device = localDeviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Local device not found: " + deviceId));
        localDeviceRepository.delete(device);
        log.info("Removed local device [{}] for user: {}", deviceId, userId);
    }
}
