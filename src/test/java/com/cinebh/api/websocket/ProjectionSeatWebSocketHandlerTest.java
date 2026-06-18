package com.cinebh.api.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectionSeatWebSocketHandlerTest {

    private static final UUID PROJECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID OTHER_PROJECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000222");
    private static final String SESSION_ID = "projection-seat-session";

    @Mock
    private WebSocketSession session;

    private ProjectionSeatWebSocketHandler projectionSeatWebSocketHandler;

    @BeforeEach
    void setUp() {
        projectionSeatWebSocketHandler = new ProjectionSeatWebSocketHandler();
    }

    @Test
    void shouldRegisterSessionAndPublishSeatMapChange() throws Exception {
        mockSession(validProjectionSeatUri(PROJECTION_ID));
        when(session.isOpen()).thenReturn(true);
        final ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);

        projectionSeatWebSocketHandler.afterConnectionEstablished(session);
        projectionSeatWebSocketHandler.publishSeatMapChanged(PROJECTION_ID);

        verify(session).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload())
                .isEqualTo("{\"type\":\"SEAT_MAP_CHANGED\",\"projectionId\":\"" + PROJECTION_ID + "\"}");
    }

    @Test
    void shouldIgnorePublishWhenNoSessionsExist() throws Exception {
        projectionSeatWebSocketHandler.publishSeatMapChanged(PROJECTION_ID);

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldCloseConnectionWhenUriIsMissing() throws Exception {
        when(session.getUri()).thenReturn(null);

        projectionSeatWebSocketHandler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
        verify(session, never()).getId();
    }

    @Test
    void shouldCloseConnectionWhenProjectionSegmentIsMissing() throws Exception {
        when(session.getUri()).thenReturn(URI.create("/api/v1/ws/seats"));

        projectionSeatWebSocketHandler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void shouldCloseConnectionWhenProjectionIdIsMissing() throws Exception {
        when(session.getUri()).thenReturn(URI.create("/api/v1/ws/projections"));

        projectionSeatWebSocketHandler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void shouldCloseConnectionWhenProjectionIdIsInvalid() throws Exception {
        when(session.getUri()).thenReturn(URI.create("/api/v1/ws/projections/not-a-uuid/seats"));

        projectionSeatWebSocketHandler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void shouldNotPublishToDifferentProjectionSessions() throws Exception {
        mockSession(validProjectionSeatUri(PROJECTION_ID));

        projectionSeatWebSocketHandler.afterConnectionEstablished(session);
        projectionSeatWebSocketHandler.publishSeatMapChanged(OTHER_PROJECTION_ID);

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldRemoveClosedSessionDuringPublish() throws Exception {
        mockSession(validProjectionSeatUri(PROJECTION_ID));
        when(session.isOpen()).thenReturn(false);

        projectionSeatWebSocketHandler.afterConnectionEstablished(session);
        projectionSeatWebSocketHandler.publishSeatMapChanged(PROJECTION_ID);
        projectionSeatWebSocketHandler.publishSeatMapChanged(PROJECTION_ID);

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldRemoveSessionWhenSendingMessageFails() throws Exception {
        mockSession(validProjectionSeatUri(PROJECTION_ID));
        when(session.isOpen()).thenReturn(true);
        doThrow(new IOException("WebSocket send failed"))
                .when(session)
                .sendMessage(any(TextMessage.class));

        projectionSeatWebSocketHandler.afterConnectionEstablished(session);
        projectionSeatWebSocketHandler.publishSeatMapChanged(PROJECTION_ID);
        projectionSeatWebSocketHandler.publishSeatMapChanged(PROJECTION_ID);

        verify(session, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldRemoveSessionAfterConnectionClosed() throws Exception {
        mockSession(validProjectionSeatUri(PROJECTION_ID));

        projectionSeatWebSocketHandler.afterConnectionEstablished(session);
        projectionSeatWebSocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL);
        projectionSeatWebSocketHandler.publishSeatMapChanged(PROJECTION_ID);

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldCloseSessionWithServerErrorAfterTransportError() throws Exception {
        mockSession(validProjectionSeatUri(PROJECTION_ID));

        projectionSeatWebSocketHandler.afterConnectionEstablished(session);
        projectionSeatWebSocketHandler.handleTransportError(session, new IOException("Connection dropped"));
        projectionSeatWebSocketHandler.publishSeatMapChanged(PROJECTION_ID);

        verify(session).close(CloseStatus.SERVER_ERROR);
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldCloseUnknownSessionWithServerErrorAfterTransportError() throws Exception {
        when(session.getId()).thenReturn(SESSION_ID);

        projectionSeatWebSocketHandler.handleTransportError(session, new IOException("Connection dropped"));

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    private void mockSession(final URI uri) {
        when(session.getUri()).thenReturn(uri);
        when(session.getId()).thenReturn(SESSION_ID);
    }

    private URI validProjectionSeatUri(final UUID projectionId) {
        return URI.create("/api/v1/ws/projections/" + projectionId + "/seats");
    }
}
