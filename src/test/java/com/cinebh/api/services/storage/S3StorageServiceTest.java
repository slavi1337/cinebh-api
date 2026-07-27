package com.cinebh.api.services.storage;

import com.cinebh.api.exceptions.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedGetObjectRequest presignedRequest;

    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        final StorageProperties storageProperties = new StorageProperties();
        storageProperties.setBucket("cinebh");
        storageProperties.setPresignedUrlTtl(Duration.ofMinutes(5));
        storageService = new S3StorageService(s3Client, s3Presigner, storageProperties);
    }

    @Test
    void shouldCreatePresignedGetUri() throws Exception {
        final URI expectedUri = URI.create(
                "https://storage.example.com/cinebh/profile-images/avatar.png?signature=test"
        );
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(expectedUri.toURL());

        final URI result = storageService.createPresignedGetUri("profile-images/avatar.png");

        final ArgumentCaptor<GetObjectPresignRequest> requestCaptor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(requestCaptor.capture());

        assertThat(result).isEqualTo(expectedUri);
        assertThat(requestCaptor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(requestCaptor.getValue().getObjectRequest().bucket()).isEqualTo("cinebh");
        assertThat(requestCaptor.getValue().getObjectRequest().key()).isEqualTo("profile-images/avatar.png");
    }

    @Test
    void shouldRejectBlankObjectKey() {
        assertThatThrownBy(() -> storageService.createPresignedGetUri(" "))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }
}
