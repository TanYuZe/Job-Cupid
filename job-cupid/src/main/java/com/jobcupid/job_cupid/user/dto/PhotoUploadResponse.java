package com.jobcupid.job_cupid.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PhotoUploadResponse {

    private String presignedUrl;
    private String publicUrl;
    private long   expiresIn;
}
