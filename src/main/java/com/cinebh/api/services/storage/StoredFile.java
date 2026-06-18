package com.cinebh.api.services.storage;

public record StoredFile(
        String contentType,
        byte[] content
) {
}
