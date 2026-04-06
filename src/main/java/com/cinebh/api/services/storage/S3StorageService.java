package com.cinebh.api.services.storage;

import com.cinebh.api.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    @Override
    public String upload(String directory, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("File must not be empty", HttpStatus.BAD_REQUEST);
        }

        try {
            final String originalFilename = StringUtils.cleanPath(
                    file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()
            );

            final String objectKey = directory + "/" + UUID.randomUUID() + "-" + originalFilename;

            final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(file.getBytes())
            );

            return objectKey;
        } catch (IOException exception) {
            throw new ApiException("Failed to read file content", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception exception) {
            throw new ApiException("Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            final DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception exception) {
            throw new ApiException("Failed to delete file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String getPublicUrl(String objectKey) {
        return storageProperties.getPublicBaseUrl() + "/" + objectKey;
    }
}
