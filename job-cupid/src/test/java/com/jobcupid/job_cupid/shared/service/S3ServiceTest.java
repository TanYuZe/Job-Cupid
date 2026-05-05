package com.jobcupid.job_cupid.shared.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobcupid.job_cupid.shared.config.AwsProperties;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock S3Presigner   s3Presigner;
    @Mock AwsProperties awsProperties;

    @InjectMocks S3Service s3Service;

    private AwsProperties.S3 s3Props;

    @BeforeEach
    void setUp() {
        s3Props = new AwsProperties.S3();
        s3Props.setBucketName("test-bucket");
        when(awsProperties.getS3()).thenReturn(s3Props);
    }

    @Test
    void generatePresignedPutUrl_keyContainsUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        String key = "photos/" + userId + "/" + UUID.randomUUID();
        URL fakeUrl = URI.create("https://test-bucket.s3.amazonaws.com/" + key + "?X-Amz-Signature=abc").toURL();

        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(fakeUrl);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        URL result = s3Service.generatePresignedPutUrl(key, "image/jpeg", Duration.ofMinutes(5));

        assertThat(result.toString()).contains(userId.toString());
    }

    @Test
    void buildPublicUrl_returnsCorrectS3Format() {
        when(awsProperties.getRegion()).thenReturn("ap-southeast-1");
        String key = "photos/user-id/image.jpg";

        String result = s3Service.buildPublicUrl(key);

        assertThat(result).isEqualTo(
                "https://test-bucket.s3.ap-southeast-1.amazonaws.com/photos/user-id/image.jpg");
    }
}
