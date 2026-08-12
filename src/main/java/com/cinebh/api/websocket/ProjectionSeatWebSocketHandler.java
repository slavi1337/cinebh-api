package com.cinebh.api.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProjectionSeatWebSocketHandler extends TextWebSocketHandler {

    private static final String EVENT_TYPE = "SEAT_MAP_CHANGED";

    private final Map<UUID, Set<WebSocketSession>> sessionsByProjection = new ConcurrentHashMap<>();
    private final Map<String, UUID> projectionBySessionId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        final Optional<UUID> projectionId = parseProjectionId(session.getUri());

        if (projectionId.isEmpty()) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        projectionBySessionId.put(session.getId(), projectionId.get());
        sessionsByProjection
                .computeIfAbsent(projectionId.get(), ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        removeSession(session);
    }

    @Override
    public void handleTransportError(final WebSocketSession session, final Throwable exception) throws Exception {
        removeSession(session);
        session.close(CloseStatus.SERVER_ERROR);
    }

    public void publishSeatMapChanged(final UUID projectionId) {
        final Set<WebSocketSession> sessions = sessionsByProjection.get(projectionId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        final TextMessage message = createMessage(projectionId);
        sessions.removeIf(session -> !sendMessage(session, message));
    }

    private TextMessage createMessage(final UUID projectionId) {
        return new TextMessage("{\"type\":\"" + EVENT_TYPE + "\",\"projectionId\":\"" + projectionId + "\"}");
    }

    private boolean sendMessage(final WebSocketSession session, final TextMessage message) {
        if (!session.isOpen()) {
            return false;
        }

        try {
            session.sendMessage(message);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private void removeSession(final WebSocketSession session) {
        final UUID projectionId = projectionBySessionId.remove(session.getId());

        if (projectionId == null) {
            return;
        }

        final Set<WebSocketSession> sessions = sessionsByProjection.get(projectionId);

        if (sessions == null) {
            return;
        }

        sessions.remove(session);

        if (sessions.isEmpty()) {
            sessionsByProjection.remove(projectionId);
        }
    }

    private Optional<UUID> parseProjectionId(final URI uri) {
        if (uri == null) {
            return Optional.empty();
        }

        final String[] segments = uri.getPath().split("/");

        for (int index = 0; index < segments.length; index++) {
            if ("projections".equals(segments[index]) && index + 1 < segments.length) {
                try {
                    return Optional.of(UUID.fromString(segments[index + 1]));
                } catch (IllegalArgumentException exception) {
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }

}
