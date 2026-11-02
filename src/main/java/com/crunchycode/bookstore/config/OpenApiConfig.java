package com.crunchycode.bookstore.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Crunchy Code Bookstore REST API",
                version = "1.0.0",
                description = "Production Capstone: High-performance Bookstore REST API featuring Spring Security 6, JWT Authentication, Method-Level RBAC, Flyway migrations, sub-500ms paginated queries at 1,000+ records, and zero internal stack trace leakage.",
                contact = @Contact(
                        name = "Crunchy Code Engineering Team",
                        email = "engineering@crunchycode.com",
                        url = "https://crunchycode.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(url = "/", description = "Current Environment Gateway")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Bearer Token obtained from POST /api/v1/auth/login. Send as 'Authorization: Bearer <token>' in header."
)
public class OpenApiConfig {
}
