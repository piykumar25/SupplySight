package com.supplysight.identity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI (Swagger) configuration for the Identity Service. */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private int serverPort;

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(
                        new Info()
                                .title("SupplySight Identity Service API")
                                .description(
                                        """
                            Identity Service for the SupplySight Supply Chain Visibility Platform.

                            ## Features
                            - Multi-tenant user and tenant management
                            - JWT-based authentication
                            - Role-based access control (ADMIN, OPS_USER, VIEWER)
                            - Secure password management with BCrypt

                            ## Authentication
                            1. POST /api/v1/login with email and password
                            2. Use the returned accessToken in the Authorization header: `Bearer <token>`
                            3. Use /api/v1/refresh to get a new access token when it expires
                            """)
                                .version("1.0.0")
                                .contact(
                                        new Contact()
                                                .name("SupplySight Team")
                                                .email("support@supplysight.com"))
                                .license(
                                        new License()
                                                .name("Apache 2.0")
                                                .url(
                                                        "https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(
                        List.of(
                                new Server()
                                        .url("http://localhost:" + serverPort)
                                        .description("Local development"),
                                new Server()
                                        .url("http://localhost:8080")
                                        .description("Via API Gateway")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "JWT token obtained from /api/v1/login")));
    }
}
