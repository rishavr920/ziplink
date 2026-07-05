package com.ziplink.dto;

import java.time.Instant;


// ShortenResponse.
// Outgoing payload returned upon successful URL creation: POST /api/shorten

public class ShortenResponse {

    private String originalUrl;
    private String shortUrl;
    private String shortCode;
    private String qrCode;
    private Instant expiresAt;
    private Boolean isPasswordProtected;
    private Boolean isOneTime;
    private String message;

    // Constructors
    public ShortenResponse() {}

    public ShortenResponse(String originalUrl, String shortUrl, String shortCode, String qrCode, 
                           Instant expiresAt, Boolean isPasswordProtected, Boolean isOneTime, String message) {
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.shortCode = shortCode;
        this.qrCode = qrCode;
        this.expiresAt = expiresAt;
        this.isPasswordProtected = isPasswordProtected;
        this.isOneTime = isOneTime;
        this.message = message;
    }

    // Getters and Setters
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getShortUrl() { return shortUrl; }
    public void setShortUrl(String shortUrl) { this.shortUrl = shortUrl; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Boolean getIsPasswordProtected() { return isPasswordProtected; }
    public void setIsPasswordProtected(Boolean passwordProtected) { isPasswordProtected = passwordProtected; }

    public Boolean getIsOneTime() { return isOneTime; }
    public void setIsOneTime(Boolean oneTime) { isOneTime = oneTime; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    // Builder
    public static ShortenResponseBuilder builder() {
        return new ShortenResponseBuilder();
    }

    public static class ShortenResponseBuilder {
        private String originalUrl;
        private String shortUrl;
        private String shortCode;
        private String qrCode;
        private Instant expiresAt;
        private Boolean isPasswordProtected;
        private Boolean isOneTime;
        private String message;

        public ShortenResponseBuilder originalUrl(String originalUrl) { this.originalUrl = originalUrl; return this; }
        public ShortenResponseBuilder shortUrl(String shortUrl) { this.shortUrl = shortUrl; return this; }
        public ShortenResponseBuilder shortCode(String shortCode) { this.shortCode = shortCode; return this; }
        public ShortenResponseBuilder qrCode(String qrCode) { this.qrCode = qrCode; return this; }
        public ShortenResponseBuilder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public ShortenResponseBuilder isPasswordProtected(Boolean isPasswordProtected) { this.isPasswordProtected = isPasswordProtected; return this; }
        public ShortenResponseBuilder isOneTime(Boolean isOneTime) { this.isOneTime = isOneTime; return this; }
        public ShortenResponseBuilder message(String message) { this.message = message; return this; }

        public ShortenResponse build() {
            return new ShortenResponse(originalUrl, shortUrl, shortCode, qrCode, expiresAt, isPasswordProtected, isOneTime, message);
        }
    }
}
