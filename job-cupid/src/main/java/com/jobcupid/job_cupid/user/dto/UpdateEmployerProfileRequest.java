package com.jobcupid.job_cupid.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateEmployerProfileRequest {

    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;

    private String companyDescription;

    @Size(max = 500, message = "Company website URL must not exceed 500 characters")
    private String companyWebsite;

    @Size(max = 500, message = "Company logo URL must not exceed 500 characters")
    private String companyLogoUrl;

    @Pattern(
        regexp = "^(1-10|11-50|51-200|201-500|501-1000|1000\\+)$",
        message = "companySize must be one of: 1-10, 11-50, 51-200, 201-500, 501-1000, 1000+"
    )
    private String companySize;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    private Short foundedYear;
}
