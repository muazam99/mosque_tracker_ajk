package com.qiyam.shared.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates critical configuration properties at startup.
 * Fails fast with a clear error message instead of cryptic NPEs later.
 */
@Slf4j
@Component
public class AppStartupValidator {

    private final AppProperties props;

    public AppStartupValidator(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    void validate() {
        requireNonBlank(props.supabase().url(), "SUPABASE_URL",
                "Supabase project URL (e.g. https://xxx.supabase.co)");
        requireNonBlank(props.supabase().serviceRoleKey(), "SUPABASE_SERVICE_ROLE_KEY",
                "Supabase service_role JWT key (from Project Settings > API)");
        requireNonBlank(props.supabase().anonKey(), "SUPABASE_ANON_KEY",
                "Supabase anon/public JWT key (from Project Settings > API)");
        requireNonBlank(props.jwt().secret(), "APP_JWT_SECRET",
                "A strong secret key (at least 256 bits) for signing backend JWT tokens");

        log.info("Critical configuration validated successfully");
        log.info("Supabase URL: {}", props.supabase().url());
        log.info("JWT issuer: {}", props.jwt().issuer());
        log.info("CORS allowed origins: {}", props.cors().allowedOrigins());
    }

    private void requireNonBlank(String value, String varName, String description) {
        if (value == null || value.isBlank()) {
            var msg = String.format(
                    "%n============================================================%n"
                            + "FATAL: Required environment variable '%s' is missing or empty.%n"
                            + "Description: %s%n"
                            + "Set this variable and restart the application.%n"
                            + "============================================================",
                    varName, description);
            log.error(msg);
            throw new IllegalStateException(
                    "Missing required environment variable: " + varName + " – " + description);
        }
    }
}
