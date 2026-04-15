package com.jobcupid.job_cupid.shared.exception;

public class SwipeLimitExceededException extends RuntimeException {

    public SwipeLimitExceededException() {
        super("Daily swipe limit reached. Upgrade to Premium for unlimited swipes.");
    }
}
