package com.ziplink.exception;

/**
 * RateLimitExceededException.
 * 
 * Thrown whenever a client exceeds the maximum rate limit (HTTP 429).
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
