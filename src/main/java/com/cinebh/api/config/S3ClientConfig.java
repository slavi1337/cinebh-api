package com.cinebh.api.config;

import com.cinebh.api.services.storage.StorageProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3ClientConfig {

    @Bean
    public S3Client s3Client(StorageProperties storageProperties) {
        final S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(storageProperties.getRegion()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        storageProperties.getAccessKey(),
                                        storageProperties.getSecretKey()
                                )
                        )
                )
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(storageProperties.isPathStyleAccessEnabled())
                                .build()
                );

        if (storageProperties.getEndpoint() != null && !storageProperties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(storageProperties.getEndpoint()));
        }

        return builder.build();
    }
}
