package com.cinebh.api.services.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String bucket;
    private String region;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String publicBaseUrl;
    private String presignEndpoint;
    private Duration presignedUrlTtl = Duration.ofMinutes(5);
    private boolean pathStyleAccessEnabled;
}
