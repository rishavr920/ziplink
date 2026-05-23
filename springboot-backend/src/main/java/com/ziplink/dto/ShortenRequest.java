package com.ziplink.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ShortenRequest.
 * 
 * Incoming payload schema for URL creation: POST /api/shorten
 */
public class ShortenRequest {

    @NotBlank(message = "URL is required")
    @Pattern(
            regexp = "^https?://.+", 
            message = "Please provide a valid URL starting with http:// or https://"
    )
    private String longUrl;

    @Size(min = 4, max = 100, message = "Password must be at least 4 characters long")
    private String password;

    private Boolean isOneTime;

    @Min(value = 1, message = "Expiry must be at least 1 hour")
    @Max(value = 8760, message = "Expiry cannot exceed 8760 hours (1 year)")
    private Integer expiryHours;

    // Constructors
    public ShortenRequest() {}

    public ShortenRequest(String longUrl, String password, Boolean isOneTime, Integer expiryHours) {
        this.longUrl = longUrl;
        this.password = password;
        this.isOneTime = isOneTime;
        this.expiryHours = expiryHours;
    }

    // Getters and Setters
    public String getLongUrl() { return longUrl; }
    public void setLongUrl(String longUrl) { this.longUrl = longUrl; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getIsOneTime() { return isOneTime; }
    public void setIsOneTime(Boolean oneTime) { isOneTime = oneTime; }

    public Integer getExpiryHours() { return expiryHours; }
    public void setExpiryHours(Integer expiryHours) { this.expiryHours = expiryHours; }
}
