package com.supplysight.identity.bootstrap;

import com.supplysight.identity.entity.Tenant;
import com.supplysight.identity.entity.User;
import com.supplysight.identity.repository.TenantRepository;
import com.supplysight.identity.repository.UserRepository;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Automatically seeds demo data on application startup. Runs only if data is missing. */
@Component
@Profile("!prod") // Don't run in production
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Hardcoded Tenant ID to ensure consistency with Visibility Seeding
    public static final UUID DEMO_TENANT_ID =
            UUID.fromString("9163981b-29ca-4765-b5ef-a1e838db11d2");

    public DataSeeder(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Checking for demo data...");

        Tenant tenant = tenantRepository.findByCode("demo-tenant").orElse(null);

        if (tenant == null) {
            log.info("Demo tenant not found. Creating...");
            tenant = new Tenant();
            tenant.setId(DEMO_TENANT_ID); // Force consistent ID
            tenant.setName("Demo Company");
            tenant.setCode("demo-tenant");
            tenant.setContactEmail("admin@demo.com");
            tenant.setContactPhone("+1-555-0100");
            tenant.setAddress("123 Demo Street, Demo City, DC 12345");
            tenant.setSettings(Map.of("timezone", "America/New_York", "language", "en"));
            tenant = tenantRepository.save(tenant);
            log.info("Created Demo Tenant with ID: {}", tenant.getId());
        } else {
            log.info("Demo tenant exists (ID: {}).", tenant.getId());
        }

        createUserIfMissing(
                tenant.getId(),
                "admin@demo.com",
                "admin",
                "admin123",
                "Admin",
                "User",
                Set.of("ADMIN"));
        createUserIfMissing(
                tenant.getId(),
                "ops@demo.com",
                "opsuser",
                "ops123",
                "Operations",
                "User",
                Set.of("OPS_USER"));
        createUserIfMissing(
                tenant.getId(),
                "viewer@demo.com",
                "viewer",
                "viewer123",
                "Viewer",
                "User",
                Set.of("VIEWER"));

        log.info("Demo data check complete.");
    }

    private void createUserIfMissing(
            UUID tenantId,
            String email,
            String username,
            String password,
            String firstName,
            String lastName,
            Set<String> roles) {
        if (!userRepository.existsByEmail(email)) {
            log.info("Creating user: {}", email);
            User user = new User();
            user.setTenantId(tenantId);
            user.setEmail(email);
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setRolesSet(roles);
            userRepository.save(user);
        }
    }
}
