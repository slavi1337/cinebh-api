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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    @Override
    public String upload(String directory, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("File must not be empty", HttpStatus.BAD_REQUEST);
        }

        final String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()
        );

        final String objectKey = directory + "/" + UUID.randomUUID() + "-" + originalFilename;

        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );

            return objectKey;
        } catch (IOException exception) {
            throw new ApiException("Failed to read file content", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception exception) {
            throw new ApiException("Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public URI createPresignedGetUri(final String objectKey) {
        validateObjectKey(objectKey);

        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(objectKey)
                .build();
        final GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(storageProperties.getPresignedUrlTtl())
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            return URI.create(s3Presigner.presignGetObject(presignRequest).url().toString());
        } catch (Exception exception) {
            throw new ApiException("Failed to create file access URL", HttpStatus.INTERNAL_SERVER_ERROR);
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

    private void validateObjectKey(final String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new ApiException("File key must not be empty", HttpStatus.BAD_REQUEST);
        }
    }
}
