package com.jobcupid.job_cupid.shared.exception;

public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException() {
        super("You have already applied to this job");
    }
}
