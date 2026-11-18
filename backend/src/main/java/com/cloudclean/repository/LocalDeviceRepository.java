package com.cloudclean.repository;

import com.cloudclean.device.LocalDevice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalDeviceRepository extends MongoRepository<LocalDevice, String> {

    List<LocalDevice> findByUserId(String userId);

    Optional<LocalDevice> findByIdAndUserId(String id, String userId);

    Optional<LocalDevice> findByUserIdAndHostname(String userId, String hostname);

    Optional<LocalDevice> findByUserIdAndDeviceName(String userId, String deviceName);

    long countByUserId(String userId);
}
