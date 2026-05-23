package com.ziplink.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * VerifyPasswordRequest.
 * 
 * Incoming payload schema for password verification: POST /api/verify-password/{code}
 */
public class VerifyPasswordRequest {

    @NotBlank(message = "Password is required")
    private String password;

    public VerifyPasswordRequest() {}

    public VerifyPasswordRequest(String password) {
        this.password = password;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
