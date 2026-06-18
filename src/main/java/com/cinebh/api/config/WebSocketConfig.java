package com.cinebh.api.config;

import com.cinebh.api.websocket.ProjectionSeatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private static final String PROJECTION_SEATS_PATH = "/api/v1/ws/projections/*/seats";

    private final ProjectionSeatWebSocketHandler projectionSeatWebSocketHandler;
    private final SecurityProperties securityProperties;

    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(
                        projectionSeatWebSocketHandler,
                        PROJECTION_SEATS_PATH
                )
                .setAllowedOrigins(securityProperties.cors().allowedOrigins().toArray(String[]::new));
    }
}
