package com.cloudclean.audit;

import com.cloudclean.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(String userId, String action, String resourceType, String resourceId, String details, boolean success) {
        AuditLog auditLog = new AuditLog(userId, action, resourceType, resourceId, details, success);
        auditLogRepository.save(auditLog);
        log.info("[AUDIT] User: {} | Action: {} | Resource: {}/{} | Success: {}", userId, action, resourceType, resourceId, success);
    }
}
