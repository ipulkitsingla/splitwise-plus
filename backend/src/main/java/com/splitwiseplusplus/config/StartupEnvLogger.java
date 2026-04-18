package com.splitwiseplusplus.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Logs startup-time environment diagnostics for deployment troubleshooting.
 * Sensitive values are masked to avoid secret leakage in logs.
 */
@Component
@Slf4j
public class StartupEnvLogger implements ApplicationRunner {

    private static final Set<String> REQUIRED_ENV_VARS = new LinkedHashSet<>(Arrays.asList(
            "PORT",
            "DB_URL",
            "DB_USERNAME",
            "DB_PASSWORD",
            "JWT_SECRET"
    ));

    private static final Set<String> OPTIONAL_ENV_VARS = new LinkedHashSet<>(Arrays.asList(
            "JWT_EXPIRATION_MS",
            "JWT_REFRESH_EXPIRATION_MS",
            "CORS_ORIGINS",
            "FIREBASE_ENABLED",
            "FIREBASE_CREDENTIALS",
            "MAIL_HOST",
            "MAIL_PORT",
            "MAIL_USERNAME",
            "MAIL_PASSWORD",
            "MAIL_FROM",
            "UPLOAD_DIR",
            "TESSDATA_PATH"
    ));

    private final Environment environment;

    public StartupEnvLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== Startup Environment Diagnostics ==========");
        log.info("Active profiles: {}", Arrays.toString(environment.getActiveProfiles()));

        REQUIRED_ENV_VARS.forEach(key -> logEnvStatus(key, true));
        OPTIONAL_ENV_VARS.forEach(key -> logEnvStatus(key, false));

        log.info("=====================================================");
    }

    private void logEnvStatus(String key, boolean required) {
        String value = environment.getProperty(key);
        boolean present = value != null && !value.isBlank();

        String category = required ? "REQUIRED" : "OPTIONAL";
        if (!present && required) {
            log.error("[{}] {} -> MISSING", category, key);
            return;
        }

        if (!present) {
            log.warn("[{}] {} -> not set", category, key);
            return;
        }

        if (isSensitive(key)) {
            log.info("[{}] {} -> set (masked): {}", category, key, maskValue(value));
        } else {
            log.info("[{}] {} -> set: {}", category, key, value);
        }
    }

    private boolean isSensitive(String key) {
        String normalized = key.toUpperCase();
        return normalized.contains("PASSWORD")
                || normalized.contains("SECRET")
                || normalized.contains("TOKEN")
                || normalized.contains("CREDENTIAL");
    }

    private String maskValue(String value) {
        if (value.length() <= 4) {
            return "****";
        }
        String suffix = value.substring(value.length() - 4);
        return "***" + suffix + " (len=" + value.length() + ")";
    }
}
