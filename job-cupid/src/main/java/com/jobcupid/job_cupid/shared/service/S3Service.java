package com.jobcupid.job_cupid.shared.service;

import java.net.URL;
import java.time.Duration;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.jobcupid.job_cupid.shared.exception.BusinessRuleException;
import com.jobcupid.job_cupid.shared.config.AwsProperties;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final S3Presigner   s3Presigner;
    private final AwsProperties awsProperties;

    public URL generatePresignedPutUrl(String key, String contentType, Duration expiry) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessRuleException("Unsupported content type: " + contentType + ". Allowed: image/jpeg, image/png");
        }

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(awsProperties.getS3().getBucketName())
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(putRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url();
    }

    public String buildPublicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                awsProperties.getS3().getBucketName(),
                awsProperties.getRegion(),
                key);
    }
}
