package com.jobcupid.job_cupid.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhotoConfirmRequest {

    @NotBlank
    @Size(max = 500)
    private String publicUrl;
}
