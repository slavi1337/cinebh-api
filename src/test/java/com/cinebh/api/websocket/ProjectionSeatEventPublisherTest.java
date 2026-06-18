package com.cinebh.api.websocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectionSeatEventPublisherTest {

    private static final UUID PROJECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000111");

    @Mock
    private ProjectionSeatWebSocketHandler projectionSeatWebSocketHandler;

    private ProjectionSeatEventPublisher projectionSeatEventPublisher;

    @BeforeEach
    void setUp() {
        projectionSeatEventPublisher = new ProjectionSeatEventPublisher(projectionSeatWebSocketHandler);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void shouldPublishImmediatelyWhenTransactionSynchronizationIsNotActive() {
        projectionSeatEventPublisher.publishSeatMapChanged(PROJECTION_ID);

        verify(projectionSeatWebSocketHandler).publishSeatMapChanged(PROJECTION_ID);
    }

    @Test
    void shouldPublishAfterCommitWhenTransactionSynchronizationIsActive() {
        TransactionSynchronizationManager.initSynchronization();

        projectionSeatEventPublisher.publishSeatMapChanged(PROJECTION_ID);

        verify(projectionSeatWebSocketHandler, never()).publishSeatMapChanged(PROJECTION_ID);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(projectionSeatWebSocketHandler).publishSeatMapChanged(PROJECTION_ID);
    }
}
