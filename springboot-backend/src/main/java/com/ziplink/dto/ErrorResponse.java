package com.ziplink.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * ErrorResponse.
 * 
 * Standardized REST API error payload.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private String error;
    private String message;
    private List<String> details;
    private Boolean requiresPassword;
    private String shortCode;

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(Instant timestamp, String error, String message, List<String> details,
                         Boolean requiresPassword, String shortCode) {
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.error = error;
        this.message = message;
        this.details = details;
        this.requiresPassword = requiresPassword;
        this.shortCode = shortCode;
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }

    public Boolean getRequiresPassword() { return requiresPassword; }
    public void setRequiresPassword(Boolean requiresPassword) { this.requiresPassword = requiresPassword; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public static ErrorResponseBuilder builder() { return new ErrorResponseBuilder(); }

    public static class ErrorResponseBuilder {
        private Instant timestamp = Instant.now();
        private String error;
        private String message;
        private List<String> details;
        private Boolean requiresPassword;
        private String shortCode;

        public ErrorResponseBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public ErrorResponseBuilder error(String error) { this.error = error; return this; }
        public ErrorResponseBuilder message(String message) { this.message = message; return this; }
        public ErrorResponseBuilder details(List<String> details) { this.details = details; return this; }
        public ErrorResponseBuilder requiresPassword(Boolean requiresPassword) { this.requiresPassword = requiresPassword; return this; }
        public ErrorResponseBuilder shortCode(String shortCode) { this.shortCode = shortCode; return this; }

        public ErrorResponse build() {
            return new ErrorResponse(timestamp, error, message, details, requiresPassword, shortCode);
        }
    }
}
