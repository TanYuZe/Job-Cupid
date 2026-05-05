package com.jobcupid.job_cupid.shared.exception;

public class PremiumRequiredException extends RuntimeException {
    public PremiumRequiredException() {
        super("This feature requires a premium subscription");
    }
}
