package com.jobcupid.job_cupid.shared.exception;

/**
 * Thrown when an operation violates a business rule (HTTP 422 Unprocessable Entity).
 * Examples: applying to a job without a prior LIKE swipe, invalid status transitions.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
