package com.cinebh.api.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProjectionSeatEventPublisher {

    private final ProjectionSeatWebSocketHandler projectionSeatWebSocketHandler;

    public void publishSeatMapChanged(final UUID projectionId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            projectionSeatWebSocketHandler.publishSeatMapChanged(projectionId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                projectionSeatWebSocketHandler.publishSeatMapChanged(projectionId);
            }
        });
    }
}
