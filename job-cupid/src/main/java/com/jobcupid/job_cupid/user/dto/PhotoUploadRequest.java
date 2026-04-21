package com.jobcupid.job_cupid.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhotoUploadRequest {

    @NotBlank
    @Pattern(regexp = "image/jpeg|image/png", message = "contentType must be image/jpeg or image/png")
    private String contentType;
}
