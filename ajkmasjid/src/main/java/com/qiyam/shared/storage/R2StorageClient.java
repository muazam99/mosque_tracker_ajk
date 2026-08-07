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
 */
@Slf4j
@Component
public class R2StorageClient {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public R2StorageClient(AppProperties appProperties) {
        var storage = appProperties.storage();
        this.bucket = storage.r2BucketName();
        this.publicBaseUrl = storage.r2BaseUrl();
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create("https://" + storage.r2AccountId() + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(storage.r2AccessKeyId(), storage.r2SecretAccessKey())))
                .build();
    }

    /**
     * Uploads the given bytes to R2 under a generated, collision-resistant key and returns
     * the public URL to store alongside the record (e.g. {@code mosque_documents.file_url}).
     * Requires {@code app.storage.r2-base-url} to be configured to a domain that actually
     * serves the bucket's objects (an R2.dev subdomain or a custom domain bound to the bucket).
     */
    public String upload(byte[] content, String originalFilename, String contentType, String category) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new StorageException(
                    "R2_BASE_URL is not configured — uploaded files would have no reachable URL");
        }
        var key = buildKey(originalFilename, category);
        try {
            var request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream")
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (S3Exception e) {
            log.error("R2 upload failed for key {}: {}", key, e.getMessage());
            throw new StorageException("Failed to upload file to R2: " + e.awsErrorDetails().errorMessage(), e);
        }
        log.info("Uploaded {} bytes to R2 key {}", content.length, key);
        var base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + "/" + key;
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
