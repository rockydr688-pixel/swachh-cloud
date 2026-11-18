package com.cloudclean.user;

import com.cloudclean.repository.UserPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UserPreferencesService {

    private static final Logger log = LoggerFactory.getLogger(UserPreferencesService.class);

    private final UserPreferencesRepository userPreferencesRepository;

    public UserPreferencesService(UserPreferencesRepository userPreferencesRepository) {
        this.userPreferencesRepository = userPreferencesRepository;
    }

    public UserPreferences getUserPreferences(String userId) {
        return userPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPreferences defaults = new UserPreferences(userId);
                    return userPreferencesRepository.save(defaults);
                });
    }

    public UserPreferences updateUserPreferences(String userId, UserPreferences update) {
        UserPreferences prefs = userPreferencesRepository.findByUserId(userId)
                .orElse(new UserPreferences(userId));

        if (update.getAutoScanFrequency() != null) prefs.setAutoScanFrequency(update.getAutoScanFrequency());
        if (update.getInactivityDaysThreshold() > 0) prefs.setInactivityDaysThreshold(update.getInactivityDaysThreshold());
        if (update.getLargeFileThresholdMb() > 0) prefs.setLargeFileThresholdMb(update.getLargeFileThresholdMb());
        if (update.getBlurVarianceThreshold() > 0) prefs.setBlurVarianceThreshold(update.getBlurVarianceThreshold());
        if (update.getProtectedExtensions() != null) prefs.setProtectedExtensions(update.getProtectedExtensions());
        prefs.setEmailAlertsEnabled(update.isEmailAlertsEnabled());
        prefs.setInAppAlertsEnabled(update.isInAppAlertsEnabled());
        if (update.getTheme() != null) prefs.setTheme(update.getTheme());
        if (update.getQuotaWarningPercent() > 0) prefs.setQuotaWarningPercent(update.getQuotaWarningPercent());
        prefs.setUpdatedAt(Instant.now());

        UserPreferences saved = userPreferencesRepository.save(prefs);
        log.info("Updated user preferences for user: {}", userId);
        return saved;
    }
}
