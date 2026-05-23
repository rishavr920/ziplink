package com.ziplink.exception;

import com.ziplink.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler.
 * 
 * Central interceptor for all application errors, producing standardized JSON payloads.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        log.warn("Validation failed for request. Errors: {}", details);

        ErrorResponse response = ErrorResponse.builder()
                .error("Validation failed")
                .message("Request input fails data integrity checks")
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource query failed: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .error(ex.getError())
                .message(ex.getMessage())
                .build();

        HttpStatus status = ex.isExpired() ? HttpStatus.GONE : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        log.warn("IP rate-limiter triggered: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .error("Too many requests")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
        log.error("An unexpected server error occurred", ex);

        ErrorResponse response = ErrorResponse.builder()
                .error("Internal server error")
                .message("Failed to process request due to a server-side error.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
