package com.splitwiseplusplus.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Application configuration — Swagger/OpenAPI docs, Firebase, and static file serving.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Smart Expense Splitter (Splitwise++) API",
                version = "1.0",
                description = "Production-ready API for intelligent group expense management. " +
                              "Features: JWT auth, OCR receipt scanning, debt simplification algorithm, " +
                              "multi-currency support, real-time WebSocket notifications.",
                contact = @Contact(name = "Splitwise++", email = "support@splitwiseplus.com"),
                license = @License(name = "MIT")
        ),
        servers = {
                @Server(url = "/api/v1", description = "Default server"),
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT token obtained from /auth/login"
)
@Slf4j
public class AppConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.firebase.credentials-path}")
    private String firebaseCredentialsPath;

    @Value("${app.firebase.enabled:false}")
    private String firebaseEnabled;

    /**
     * Serve uploaded files as static resources.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

    /**
     * Initialize Firebase Admin SDK if enabled.
     */
    @PostConstruct
    public void initializeFirebase() {
        if (!isFirebaseEnabled()) {
            log.info("Firebase disabled — skipping initialization");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) return;

        try {
            InputStream serviceAccount;
            if (Files.exists(Paths.get(firebaseCredentialsPath))) {
                serviceAccount = Files.newInputStream(Paths.get(firebaseCredentialsPath));
            } else {
                serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully");
        } catch (IOException e) {
            log.warn("Firebase initialization failed: {}. Push notifications disabled.", e.getMessage());
        }
    }

    private boolean isFirebaseEnabled() {
        return Boolean.parseBoolean(firebaseEnabled == null ? "false" : firebaseEnabled.trim());
    }
}
