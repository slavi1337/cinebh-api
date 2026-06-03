package com.cinebh.api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(NominatimProperties.class)
public class NominatimClientConfig {

    @Bean
    @Qualifier("nominatimRestClient")
    public RestClient nominatimRestClient(final NominatimProperties properties) {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.REFERER, "https://cinebh.com")
                .build();
    }
}
