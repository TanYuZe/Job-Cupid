package com.jobcupid.job_cupid.shared.exception;

public class InvalidCredentialsException extends RuntimeException {

    // Fixed message — never reveal whether email or password was wrong (prevents user enumeration)
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
