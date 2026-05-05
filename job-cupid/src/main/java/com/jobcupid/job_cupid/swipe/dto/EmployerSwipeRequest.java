package com.jobcupid.job_cupid.swipe.dto;

import com.jobcupid.job_cupid.swipe.entity.EmployerSwipeAction;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmployerSwipeRequest {

    @NotNull(message = "action must be LIKE or REJECT")
    private EmployerSwipeAction action;
}
