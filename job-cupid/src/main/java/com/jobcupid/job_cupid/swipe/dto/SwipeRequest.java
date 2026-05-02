package com.jobcupid.job_cupid.swipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SwipeRequest {

    @NotBlank
    @Pattern(regexp = "LIKE|PASS", message = "action must be LIKE or PASS")
    private String action;
}
