package com.jobcupid.job_cupid.shared.exception;

public class UserBannedException extends RuntimeException {

    public UserBannedException() {
        super("Your account has been suspended. Please contact support.");
    }
}
