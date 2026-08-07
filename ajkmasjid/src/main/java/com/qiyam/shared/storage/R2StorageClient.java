package com.qiyam.shared.storage;

import com.qiyam.shared.config.AppProperties;
import com.qiyam.shared.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Uploads files to Cloudflare R2. R2 exposes an S3-compatible API, so this uses the
 * official AWS S3 SDK pointed at R2's endpoint rather than hand-rolling SigV4 signing.
 *
 * <p>The R2 credentials are optional application config (see {@code application.yml}), so the
 * {@link S3Client} is built lazily on first upload rather than in the constructor — otherwise
 * the whole application would fail to start in any environment where R2 isn't configured yet
 * (local dev, a fresh deploy before secrets are set, etc).
 */
@Slf4j
@Component
public class R2StorageClient {

    private final AppProperties.Storage storage;
    private volatile S3Client s3Client;

    public R2StorageClient(AppProperties appProperties) {
        this.storage = appProperties.storage();
    }

    /**
     * Uploads the given bytes to R2 under a generated, collision-resistant key and returns
     * the public URL to store alongside the record (e.g. {@code mosque_documents.file_url}).
     * Requires {@code app.storage.r2-*} to be fully configured — throws {@link StorageException}
     * (not a fabricated/placeholder URL) if it isn't.
     */
    public String upload(byte[] content, String originalFilename, String contentType, String category) {
        if (storage.r2BaseUrl() == null || storage.r2BaseUrl().isBlank()) {
            throw new StorageException(
                    "R2_BASE_URL is not configured — uploaded files would have no reachable URL");
        }
        var key = buildKey(originalFilename, category);
        try {
            var request = PutObjectRequest.builder()
                    .bucket(storage.r2BucketName())
                    .key(key)
                    .contentType(contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream")
                    .build();
            client().putObject(request, RequestBody.fromBytes(content));
        } catch (S3Exception e) {
            log.error("R2 upload failed for key {}: {}", key, e.getMessage());
            throw new StorageException("Failed to upload file to R2: " + e.awsErrorDetails().errorMessage(), e);
        }
        log.info("Uploaded {} bytes to R2 key {}", content.length, key);
        var base = storage.r2BaseUrl().endsWith("/")
                ? storage.r2BaseUrl().substring(0, storage.r2BaseUrl().length() - 1) : storage.r2BaseUrl();
        return base + "/" + key;
    }

    private S3Client client() {
        var client = s3Client;
        if (client == null) {
            synchronized (this) {
                client = s3Client;
                if (client == null) {
                    if (storage.r2AccountId() == null || storage.r2AccountId().isBlank()
                            || storage.r2AccessKeyId() == null || storage.r2AccessKeyId().isBlank()
                            || storage.r2SecretAccessKey() == null || storage.r2SecretAccessKey().isBlank()) {
                        throw new StorageException(
                                "R2 storage is not configured (R2_ACCOUNT_ID / R2_ACCESS_KEY_ID / R2_SECRET_ACCESS_KEY)");
                    }
                    client = S3Client.builder()
                            .endpointOverride(URI.create("https://" + storage.r2AccountId() + ".r2.cloudflarestorage.com"))
                            .region(Region.of("auto"))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(storage.r2AccessKeyId(), storage.r2SecretAccessKey())))
                            .build();
                    s3Client = client;
                }
            }
        }
        return client;
    }

    private String buildKey(String originalFilename, String category) {
        var prefix = category != null && !category.isBlank() ? sanitize(category) : "general";
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        return "documents/%s/%d-%s-%s".formatted(prefix, Instant.now().toEpochMilli(), suffix, sanitize(originalFilename));
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) return "file";
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
