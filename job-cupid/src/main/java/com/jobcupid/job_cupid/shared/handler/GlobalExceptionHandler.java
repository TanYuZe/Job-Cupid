package com.jobcupid.job_cupid.shared.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.jobcupid.job_cupid.shared.dto.ErrorResponse;
import com.jobcupid.job_cupid.shared.exception.BusinessRuleException;
import com.jobcupid.job_cupid.shared.exception.DuplicateEmailException;
import com.jobcupid.job_cupid.shared.exception.InvalidCredentialsException;
import com.jobcupid.job_cupid.shared.exception.InvalidTokenException;
import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.shared.exception.SwipeLimitExceededException;
import com.jobcupid.job_cupid.shared.exception.UserBannedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 400 Bad Request ───────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<ErrorResponse.FieldViolation> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldViolation.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .status(400)
                        .error("Validation Failed")
                        .message("Request contains invalid fields")
                        .path(extractPath(request))
                        .violations(violations)
                        .build());
    }

    // ── 401 Unauthorized ─────────────────────────────────────────────────────
    @ExceptionHandler({InvalidCredentialsException.class, InvalidTokenException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            RuntimeException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .status(401)
                        .error("Unauthorized")
                        .message(ex.getMessage())
                        .path(extractPath(request))
                        .build());
    }

    // ── 403 Forbidden ─────────────────────────────────────────────────────────
    @ExceptionHandler({AccessDeniedException.class, UserBannedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(
            RuntimeException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .status(403)
                        .error("Forbidden")
                        .message(ex.getMessage())
                        .path(extractPath(request))
                        .build());
    }

    // ── 404 Not Found ─────────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .status(404)
                        .error("Not Found")
                        .message(ex.getMessage())
                        .path(extractPath(request))
                        .build());
    }

    // ── 409 Conflict ─────────────────────────────────────────────────────────
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            DuplicateEmailException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .status(409)
                        .error("Conflict")
                        .message(ex.getMessage())
                        .path(extractPath(request))
                        .build());
    }

    // ── 422 Unprocessable Entity ──────────────────────────────────────────────
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex, WebRequest request) {

        return ResponseEntity.status(422).body(
                ErrorResponse.builder()
                        .status(422)
                        .error("Business Rule Violation")
                        .message(ex.getMessage())
                        .path(extractPath(request))
                        .build());
    }

    // ── 429 Too Many Requests ─────────────────────────────────────────────────
    @ExceptionHandler(SwipeLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(
            SwipeLimitExceededException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                ErrorResponse.builder()
                        .status(429)
                        .error("Rate Limit Exceeded")
                        .message(ex.getMessage())
                        .path(extractPath(request))
                        .build());
    }

    // ── 500 Internal Server Error ────────────────────────────────────────────
    // No stack trace in the body — only a traceId for log correlation.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, WebRequest request) {

        String traceId = org.slf4j.MDC.get("traceId");
        log.error("Unhandled exception [traceId={}]: {}", traceId, ex.getMessage(), ex);

        return ResponseEntity.internalServerError().body(
                ErrorResponse.builder()
                        .status(500)
                        .error("Internal Server Error")
                        .message("An unexpected error occurred. Reference: " + traceId)
                        .path(extractPath(request))
                        .traceId(traceId)
                        .build());
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
