package com.cinebh.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    private String fromAddress;
    private String fromName;
    private int verificationCodeTtlMinutes;
}
