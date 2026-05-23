package com.ziplink.exception;

/**
 * ResourceNotFoundException.
 * 
 * Thrown whenever a lookup for a short link or document fails.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String error;
    private final boolean expired; // Used to distinguish between 404 (Not Found) and 410 (Expired/Gone)

    public ResourceNotFoundException(String error, String message) {
        super(message);
        this.error = error;
        this.expired = false;
    }

    public ResourceNotFoundException(String error, String message, boolean expired) {
        super(message);
        this.error = error;
        this.expired = expired;
    }

    public String getError() {
        return error;
    }

    public boolean isExpired() {
        return expired;
    }
}
