Login failed. Please register via Qiyam mobile apps first.package com.qiyam.shared.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Security security,
        Cors cors,
        Supabase supabase,
        Storage storage,
        Google google
) {
    public record Jwt(@NotBlank String secret, long expirationMs, String issuer) {}
    public record Security(List<String> permittedPaths, boolean swaggerEnabled) {}
    public record Cors(List<String> allowedOrigins) {}
    public record Supabase(String url, String anonKey, String serviceRoleKey) {}
    public record Storage(String r2AccountId, String r2AccessKeyId, String r2SecretAccessKey, String r2BucketName, String r2BaseUrl) {}
    public record Google(String placesApiKey, String webClientId) {}
}
