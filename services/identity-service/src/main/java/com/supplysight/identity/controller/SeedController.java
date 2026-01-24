package com.supplysight.identity.controller;

import com.supplysight.common.dto.ApiResponse;
import com.supplysight.identity.dto.TenantDto;
import com.supplysight.identity.dto.UserDto;
import com.supplysight.identity.entity.Tenant;
import com.supplysight.identity.entity.User;
import com.supplysight.identity.repository.TenantRepository;
import com.supplysight.identity.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for seeding demo data in development environments.
 */
@RestController
@RequestMapping("/api/v1/seed")
@Tag(name = "Seed", description = "Demo data seeding (development only)")
public class SeedController {

    private static final Logger log = LoggerFactory.getLogger(SeedController.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedController(TenantRepository tenantRepository, UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/demo")
    @Operation(summary = "Seed Demo Data", description = "Create demo tenant and users for testing")
    @Transactional
    public ResponseEntity<ApiResponse<SeedResponse>> seedDemo() {
        log.info("Seeding demo data...");

        // Check if demo tenant already exists
        if (tenantRepository.existsByCode("demo-tenant")) {
            log.info("Demo tenant already exists, skipping seed");
            Tenant existingTenant = tenantRepository.findByCode("demo-tenant").orElseThrow();
            return ResponseEntity.ok(ApiResponse.success(new SeedResponse(
                    "Demo data already exists",
                    existingTenant.getId().toString(),
                    List.of()
            )));
        }

        // Create demo tenant
        Tenant tenant = new Tenant();
        tenant.setName("Demo Company");
        tenant.setCode("demo-tenant");
        tenant.setContactEmail("admin@demo.com");
        tenant.setContactPhone("+1-555-0100");
        tenant.setAddress("123 Demo Street, Demo City, DC 12345");
        tenant.setSettings(Map.of(
                "timezone", "America/New_York",
                "language", "en"
        ));
        tenant = tenantRepository.save(tenant);

        // Create demo users
        User adminUser = createUser(tenant.getId(), "admin@demo.com", "admin", "admin123", 
                "Admin", "User", Set.of("ADMIN"));
        User opsUser = createUser(tenant.getId(), "ops@demo.com", "opsuser", "ops123",
                "Operations", "User", Set.of("OPS_USER"));
        User viewerUser = createUser(tenant.getId(), "viewer@demo.com", "viewer", "viewer123",
                "Viewer", "User", Set.of("VIEWER"));

        log.info("Demo data seeded successfully");

        return ResponseEntity.ok(ApiResponse.success(new SeedResponse(
                "Demo data created successfully",
                tenant.getId().toString(),
                List.of(
                        new UserCredentials(adminUser.getEmail(), "admin123", adminUser.getRolesSet()),
                        new UserCredentials(opsUser.getEmail(), "ops123", opsUser.getRolesSet()),
                        new UserCredentials(viewerUser.getEmail(), "viewer123", viewerUser.getRolesSet())
                )
        )));
    }

    @PostMapping("/additional-tenant")
    @Operation(summary = "Seed Additional Tenant", description = "Create an additional tenant for multi-tenant testing")
    @Transactional
    public ResponseEntity<ApiResponse<SeedResponse>> seedAdditionalTenant() {
        log.info("Seeding additional tenant...");

        String tenantCode = "tenant-" + System.currentTimeMillis();
        
        Tenant tenant = new Tenant();
        tenant.setName("Test Company " + tenantCode);
        tenant.setCode(tenantCode);
        tenant.setContactEmail("admin@" + tenantCode + ".com");
        tenant = tenantRepository.save(tenant);

        User adminUser = createUser(tenant.getId(), "admin@" + tenantCode + ".com", "admin", 
                "password123", "Test", "Admin", Set.of("ADMIN"));

        log.info("Additional tenant {} created", tenantCode);

        return ResponseEntity.ok(ApiResponse.success(new SeedResponse(
                "Additional tenant created",
                tenant.getId().toString(),
                List.of(new UserCredentials(adminUser.getEmail(), "password123", adminUser.getRolesSet()))
        )));
    }

    private User createUser(java.util.UUID tenantId, String email, String username, String password,
                            String firstName, String lastName, Set<String> roles) {
        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRolesSet(roles);
        return userRepository.save(user);
    }

    public record SeedResponse(
            String message,
            String tenantId,
            List<UserCredentials> users
    ) {}

    public record UserCredentials(
            String email,
            String password,
            Set<String> roles
    ) {}
}
