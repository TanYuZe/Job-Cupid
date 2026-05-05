package com.jobcupid.job_cupid.application.dto;

import com.jobcupid.job_cupid.application.entity.ApplicationStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {
    @NotNull(message = "status is required")
    private ApplicationStatus newStatus;
}
