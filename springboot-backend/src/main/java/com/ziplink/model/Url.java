package com.ziplink.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Url Document Model.
 * 
 * Maps directly to the MongoDB "urls" collection.
 * Uses standard vanilla Java patterns (getters, setters, constructor, builder)
 * to ensure 100% build reliability under any modern Java environment.
 */
@Document(collection = "urls")
@CompoundIndex(
        name = "url_unique_idx", 
        def = "{'originalUrl': 1, 'isOneTime': 1, 'password': 1}", 
        unique = true, 
        sparse = true
)
public class Url {

    @Id
    private String id;

    @Field("originalUrl")
    private String originalUrl;

    @Indexed(unique = true)
    @Field("shortCode")
    private String shortCode;

    @Field("password")
    private String password;

    @Field("isOneTime")
    private boolean isOneTime = false;

    @Field("expiresAt")
    private Instant expiresAt;

    @Field("clicks")
    private long clicks = 0L;

    @Field("qrCode")
    private String qrCode;

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private Instant updatedAt;

    // Constructors
    public Url() {}

    public Url(String id, String originalUrl, String shortCode, String password, boolean isOneTime,
               Instant expiresAt, long clicks, String qrCode, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.password = password;
        this.isOneTime = isOneTime;
        this.expiresAt = expiresAt;
        this.clicks = clicks;
        this.qrCode = qrCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isOneTime() { return isOneTime; }
    public void setOneTime(boolean oneTime) { isOneTime = oneTime; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public long getClicks() { return clicks; }
    public void setClicks(long clicks) { this.clicks = clicks; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Explicit manual Builder Pattern
    public static UrlBuilder builder() {
        return new UrlBuilder();
    }

    public static class UrlBuilder {
        private String id;
        private String originalUrl;
        private String shortCode;
        private String password;
        private boolean isOneTime = false;
        private Instant expiresAt;
        private long clicks = 0L;
        private String qrCode;
        private Instant createdAt;
        private Instant updatedAt;

        public UrlBuilder id(String id) { this.id = id; return this; }
        public UrlBuilder originalUrl(String originalUrl) { this.originalUrl = originalUrl; return this; }
        public UrlBuilder shortCode(String shortCode) { this.shortCode = shortCode; return this; }
        public UrlBuilder password(String password) { this.password = password; return this; }
        public UrlBuilder isOneTime(boolean isOneTime) { this.isOneTime = isOneTime; return this; }
        public UrlBuilder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public UrlBuilder clicks(long clicks) { this.clicks = clicks; return this; }
        public UrlBuilder qrCode(String qrCode) { this.qrCode = qrCode; return this; }
        public UrlBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public UrlBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Url build() {
            return new Url(id, originalUrl, shortCode, password, isOneTime, expiresAt, clicks, qrCode, createdAt, updatedAt);
        }
    }
}
