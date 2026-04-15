package com.jobcupid.job_cupid.shared.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Token is invalid or has expired");
    }

    public InvalidTokenException(String message) {
        super(message);
    }
}
