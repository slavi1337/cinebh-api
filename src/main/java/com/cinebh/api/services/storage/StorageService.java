package com.cinebh.api.services.storage;

import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

public interface StorageService {

    String upload(String directory, MultipartFile file);

    URI createPresignedGetUri(String objectKey);

    void delete(String objectKey);

    String getPublicUrl(String objectKey);
}
