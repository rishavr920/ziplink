package com.ziplink.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziplink.dto.*;
import com.ziplink.exception.RateLimitExceededException;
import com.ziplink.exception.ResourceNotFoundException;
import com.ziplink.model.Url;
import com.ziplink.repository.UrlRepository;
import com.ziplink.utils.Base62;
import com.ziplink.utils.QRCodeGenerator;
import com.ziplink.utils.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * UrlService class.
 * 
 * Contains all business logic for short URL generation, cache lookups,
 * password matching, and analytics click logs.
 */
@Service
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private Snowflake snowflake;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${ziplink.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Replicates Node's shortenUrl controller logic.
     */
    public ShortenResponse shortenUrl(ShortenRequest request) {
        String longUrl = request.getLongUrl();
        boolean isOneTime = request.getIsOneTime() != null && request.getIsOneTime();
        String rawPassword = request.getPassword();
        Integer expiryHours = request.getExpiryHours();

        try {
            // 1. Deduplication Precheck (Only applies to standard links with no password)
            if (rawPassword == null && !isOneTime) {
                Optional<Url> existing = urlRepository.findByOriginalUrlAndIsOneTimeAndPassword(longUrl, false, null);
                if (existing.isPresent()) {
                    Url url = existing.get();
                    log.info("Found pre-existing short link for URL: {} -> shortCode: {}", longUrl, url.getShortCode());
                    return ShortenResponse.builder()
                            .originalUrl(url.getOriginalUrl())
                            .shortUrl(baseUrl + "/" + url.getShortCode())
                            .shortCode(url.getShortCode())
                            .qrCode(url.getQrCode())
                            .expiresAt(url.getExpiresAt())
                            .isPasswordProtected(false)
                            .isOneTime(false)
                            .message("Existing short link returned")
                            .build();
                }
            }

            // 2. Generate Unique Snowflake ID and Base62 encode it
            long id = snowflake.nextId();
            String shortCode = Base62.encode(id);
            String shortUrl = baseUrl + "/" + shortCode;

            // 3. Generate base64 QR Code string
            String qrCode = QRCodeGenerator.generateQRCode(shortUrl);

            // 4. Construct DB Entity
            Instant expiresAt = expiryHours != null ? Instant.now().plus(Duration.ofHours(expiryHours)) : null;
            String hashedPassword = rawPassword != null ? passwordEncoder.encode(rawPassword) : null;

            Url newUrl = Url.builder()
                    .originalUrl(longUrl)
                    .shortCode(shortCode)
                    .password(hashedPassword)
                    .isOneTime(isOneTime)
                    .expiresAt(expiresAt)
                    .qrCode(qrCode)
                    .clicks(0L)
                    .build();

            urlRepository.save(newUrl);
            log.info("Successfully shortened URL: {} -> shortCode: {}", longUrl, shortCode);

            // 5. Cache mapping in Redis for 1 hour (only if it is a standard non-protected link)
            if (rawPassword == null && !isOneTime) {
                long ttlSeconds = expiryHours != null ? expiryHours * 3600L : 3600L;
                cacheLink(shortCode, longUrl, false, ttlSeconds);
            }

            return ShortenResponse.builder()
                    .originalUrl(newUrl.getOriginalUrl())
                    .shortUrl(shortUrl)
                    .shortCode(newUrl.getShortCode())
                    .qrCode(newUrl.getQrCode())
                    .expiresAt(newUrl.getExpiresAt())
                    .isPasswordProtected(rawPassword != null)
                    .isOneTime(newUrl.isOneTime())
                    .build();

        } catch (Exception e) {
            log.error("Failed to shorten URL: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to shorten URL", e);
        }
    }

    /**
     * Retrieves redirect metadata for the React dynamic router.
     */
    public RedirectResponse getRedirectInfo(String shortCode) {
        // 1. Check Redis Cache
        String cachedJson = stringRedisTemplate.opsForValue().get(shortCode);
        if (cachedJson != null) {
            try {
                Map<?, ?> parsed = objectMapper.readValue(cachedJson, Map.class);
                String longUrl = (String) parsed.get("url");
                
                // Fire-and-forget: increment clicks asynchronously
                incrementClicksAsync(shortCode);

                return RedirectResponse.builder()
                        .redirect(true)
                        .url(longUrl)
                        .build();
            } catch (Exception e) {
                log.error("Failed to parse cached Redis JSON: {}", e.getMessage());
            }
        }

        // 2. Database Fallback lookup
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found", 
                        "This shortened link does not exist or has been deleted."));

        // 3. Expiration enforcement
        if (url.getExpiresAt() != null && Instant.now().isAfter(url.getExpiresAt())) {
            deleteLink(url);
            throw new ResourceNotFoundException("Link expired", "This shortened link has expired.", true);
        }

        // 4. Password Protection Check
        if (url.getPassword() != null) {
            return RedirectResponse.builder()
                    .requiresPassword(true)
                    .shortCode(shortCode)
                    .build();
        }

        // 5. Successful redirect metadata mapping
        return RedirectResponse.builder()
                .redirect(true)
                .url(url.getOriginalUrl())
                .build();
    }

    /**
     * Authenticates password input for password-protected links.
     */
    public VerifyPasswordResponse verifyPassword(String shortCode, String password, String clientIp) {
        String attemptsKey = String.format("pw_attempts:%s:%s", shortCode, clientIp);

        try {
            // 1. Brute-force rate limiting (max 5 attempts per minute)
            Long attempts = stringRedisTemplate.opsForValue().increment(attemptsKey);
            if (attempts != null) {
                if (attempts == 1L) {
                    stringRedisTemplate.expire(attemptsKey, 60, TimeUnit.SECONDS);
                }
                if (attempts > 5L) {
                    throw new RateLimitExceededException("Too many attempts. Try again later.");
                }
            }

            // 2. Lookup URL
            Url url = urlRepository.findByShortCode(shortCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Link not found", "This shortened link does not exist."));

            // 3. Check expiration
            if (url.getExpiresAt() != null && Instant.now().isAfter(url.getExpiresAt())) {
                deleteLink(url);
                throw new ResourceNotFoundException("Link expired", "This shortened link has expired.", true);
            }

            if (url.getPassword() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This link is not password protected.");
            }

            // 4. Match hashes
            if (!passwordEncoder.matches(password, url.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The password you entered is incorrect.");
            }

            // 5. Successful verification: reset brute-force counter
            stringRedisTemplate.delete(attemptsKey);

            // 6. Handle clicks and single-use self-deletion
            if (url.isOneTime()) {
                deleteLink(url);
            } else {
                url.setClicks(url.getClicks() + 1);
                urlRepository.save(url);
            }

            return VerifyPasswordResponse.builder()
                    .success(true)
                    .originalUrl(url.getOriginalUrl())
                    .isOneTime(url.isOneTime())
                    .build();

        } catch (ResponseStatusException | ResourceNotFoundException | RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify password for code: {}", shortCode, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify password.", e);
        }
    }

    /**
     * Core Direct redirect logic (invoked via GET /{code}).
     */
    public String resolveDirectRedirect(String shortCode) {
        // 1. Check Redis Cache
        String cachedJson = stringRedisTemplate.opsForValue().get(shortCode);
        if (cachedJson != null) {
            try {
                Map<?, ?> parsed = objectMapper.readValue(cachedJson, Map.class);
                String longUrl = (String) parsed.get("url");
                incrementClicksAsync(shortCode);
                return longUrl;
            } catch (Exception e) {
                log.error("Failed to parse cached Redis JSON: {}", e.getMessage());
            }
        }

        // 2. Database Fallback lookup
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found", 
                        "This shortened link does not exist or has been deleted."));

        // 3. Expiration enforcement
        if (url.getExpiresAt() != null && Instant.now().isAfter(url.getExpiresAt())) {
            deleteLink(url);
            throw new ResourceNotFoundException("Link expired", "This shortened link has expired.", true);
        }

        // 4. Password Protection Block
        if (url.getPassword() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This link requires a password to access.");
        }

        // 5. One-time self deletion vs Standard click log
        if (url.isOneTime()) {
            deleteLink(url);
        } else {
            url.setClicks(url.getClicks() + 1);
            urlRepository.save(url);

            // 6. Cache standard mappings
            long ttlSeconds = 3600L;
            if (url.getExpiresAt() != null) {
                ttlSeconds = Math.max(5L, Duration.between(Instant.now(), url.getExpiresAt()).toSeconds());
            }
            cacheLink(shortCode, url.getOriginalUrl(), false, ttlSeconds);
        }

        return url.getOriginalUrl();
    }

    // ==========================================
    // PRIVATE BACKEND UTILITIES & CACHING HELPERS
    // ==========================================

    private void cacheLink(String shortCode, String longUrl, boolean isOneTime, long ttlSeconds) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("url", longUrl);
            payload.put("isOneTime", isOneTime);
            String json = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.opsForValue().set(shortCode, json, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Cached shortCode: {} in Redis (TTL: {}s)", shortCode, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to write to Redis cache: {}", e.getMessage());
        }
    }

    private void deleteLink(Url url) {
        urlRepository.delete(url);
        stringRedisTemplate.delete(url.getShortCode());
        log.info("Deleted shortCode mapping: {}", url.getShortCode());
    }

    private void incrementClicksAsync(String shortCode) {
        CompletableFuture.runAsync(() -> {
            try {
                urlRepository.findByShortCode(shortCode).ifPresent(url -> {
                    url.setClicks(url.getClicks() + 1);
                    urlRepository.save(url);
                    log.debug("Asynchronously incremented clicks for code: {}", shortCode);
                });
            } catch (Exception e) {
                log.error("Failed to async increment click count: {}", e.getMessage());
            }
        });
    }
}
