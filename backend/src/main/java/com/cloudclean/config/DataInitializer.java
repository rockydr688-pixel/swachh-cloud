package com.cloudclean.config;

import com.cloudclean.file.FileService;
import com.cloudclean.recommendation.RecommendationService;
import com.cloudclean.repository.UserRepository;
import com.cloudclean.storage.StorageConnectionService;
import com.cloudclean.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;
    private final StorageConnectionService storageConnectionService;
    private final RecommendationService recommendationService;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           FileService fileService,
                           StorageConnectionService storageConnectionService,
                           RecommendationService recommendationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileService = fileService;
        this.storageConnectionService = storageConnectionService;
        this.recommendationService = recommendationService;
    }

    @Override
    public void run(String... args) {
        try {
            final String defaultEmail = "rocky@cloudclean.ai";
            if (!userRepository.existsByEmail(defaultEmail)) {
                log.info("Bootstrapping initial administrator account: {}", defaultEmail);
                User admin = new User();
                admin.setEmail(defaultEmail);
                admin.setPasswordHash(passwordEncoder.encode("demopassword123"));
                admin.setFullName("Rocky Balboa");
                admin.setCompany("CloudClean Enterprise Labs");
                admin.setRole("ROLE_ADMIN");
                admin.setRoles(Set.of("ROLE_ADMIN", "ROLE_USER"));
                admin.setEnabled(true);
                admin.setAvatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80");

                User saved = userRepository.save(admin);

                // Initialize default resources for admin user
                fileService.ensureInitialFilesForUser(saved.getId());
                storageConnectionService.getUserConnections(saved.getId());
                recommendationService.getUserRecommendations(saved.getId());

                log.info("Default administrator account created successfully with ID: {}", saved.getId());
            }
        } catch (Exception e) {
            log.warn("Database initialization check: MongoDB may be offline or initializing. {}", e.getMessage());
        }
    }
}
