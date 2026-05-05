package com.jobcupid.job_cupid.swipe.dto;

import com.jobcupid.job_cupid.swipe.entity.SwipeAction;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CandidateSwipeRequest {

    @NotNull(message = "action must be LIKE or PASS")
    private SwipeAction action;
}
