package com.matador.shared.storage;

import java.net.URI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    public StoragePort storagePort(
        @Value("${matador.storage.r2-account-id:}") String accountId,
        @Value("${matador.storage.r2-access-key:}") String accessKey,
        @Value("${matador.storage.r2-secret-key:}") String secretKey,
        @Value("${matador.storage.r2-bucket:}") String bucket,
        @Value("${matador.storage.r2-public-base-url:}") String publicBaseUrl) {
        if (accessKey.isBlank() || secretKey.isBlank() || bucket.isBlank()) {
            log.warn("No R2 storage configured; using mock storage (presigned URLs are placeholders).");
            return new MockStoragePort();
        }
        S3Presigner presigner =
            S3Presigner.builder()
                .region(Region.of("auto"))
                .endpointOverride(URI.create("https://%s.r2.cloudflarestorage.com".formatted(accountId)))
                .credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        return (key, contentType) -> {
            PutObjectRequest objectRequest =
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build();
            PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .putObjectRequest(objectRequest)
                    .build();
            String uploadUrl = presigner.presignPutObject(presignRequest).url().toString();
            String publicUrl = publicBaseUrl.replaceAll("/$", "") + "/" + key;
            return new StoragePort.PresignedUpload(key, uploadUrl, publicUrl);
        };
    }

    /** Placeholder storage used when R2 is not configured. */
    static class MockStoragePort implements StoragePort {
        @Override
        public PresignedUpload presignUpload(String key, String contentType) {
            String base = "https://mock-storage.matador.local";
            return new PresignedUpload(key, base + "/upload/" + key, base + "/" + key);
        }
    }
}
