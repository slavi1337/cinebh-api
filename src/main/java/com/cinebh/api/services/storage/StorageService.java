package com.cinebh.api.services.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String upload(String directory, MultipartFile file);

    StoredFile download(String objectKey);

    void delete(String objectKey);

    String getPublicUrl(String objectKey);
}
