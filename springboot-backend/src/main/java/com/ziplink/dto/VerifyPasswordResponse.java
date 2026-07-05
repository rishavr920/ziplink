package com.ziplink.dto;

//VerifyPasswordResponse.
//Outgoing payload returned upon successful password verification: POST /api/verify-password/{code}

public class VerifyPasswordResponse {

    private boolean success;
    private String originalUrl;
    private boolean isOneTime;

    public VerifyPasswordResponse() {}

    public VerifyPasswordResponse(boolean success, String originalUrl, boolean isOneTime) {
        this.success = success;
        this.originalUrl = originalUrl;
        this.isOneTime = isOneTime;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public boolean isOneTime() { return isOneTime; }
    public void setOneTime(boolean oneTime) { isOneTime = oneTime; }

    public static VerifyPasswordResponseBuilder builder() { return new VerifyPasswordResponseBuilder(); }

    public static class VerifyPasswordResponseBuilder {
        private boolean success;
        private String originalUrl;
        private boolean isOneTime;

        public VerifyPasswordResponseBuilder success(boolean success) { this.success = success; return this; }
        public VerifyPasswordResponseBuilder originalUrl(String originalUrl) { this.originalUrl = originalUrl; return this; }
        public VerifyPasswordResponseBuilder isOneTime(boolean isOneTime) { this.isOneTime = isOneTime; return this; }

        public VerifyPasswordResponse build() {
            return new VerifyPasswordResponse(success, originalUrl, isOneTime);
        }
    }
}
