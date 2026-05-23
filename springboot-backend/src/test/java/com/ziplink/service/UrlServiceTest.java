package com.ziplink.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziplink.dto.*;
import com.ziplink.exception.RateLimitExceededException;
import com.ziplink.exception.ResourceNotFoundException;
import com.ziplink.model.Url;
import com.ziplink.repository.UrlRepository;
import com.ziplink.utils.Snowflake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private Snowflake snowflake;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        // Manually inject baseUrl configuration value into UrlService
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:3000");
    }

    @Test
    @DisplayName("Shortening an existing standard link should return the pre-existing mapping directly without recreation")
    void shortenExistingUrl() {
        ShortenRequest request = new ShortenRequest("https://google.com", null, false, null);
        Url preExisting = Url.builder()
                .originalUrl("https://google.com")
                .shortCode("abc")
                .qrCode("qrCodeBase64")
                .isOneTime(false)
                .build();

        when(urlRepository.findByOriginalUrlAndIsOneTimeAndPassword("https://google.com", false, null))
                .thenReturn(Optional.of(preExisting));

        ShortenResponse response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("abc", response.getShortCode());
        assertEquals("http://localhost:3000/abc", response.getShortUrl());
        assertEquals("Existing short link returned", response.getMessage());
        verify(urlRepository, never()).save(any());
    }

    @Test
    @DisplayName("Shortening a new URL should save mapping and trigger Redis caching")
    void shortenNewUrl() {
        ShortenRequest request = new ShortenRequest("https://google.com", null, false, null);
        when(urlRepository.findByOriginalUrlAndIsOneTimeAndPassword(anyString(), anyBoolean(), any()))
                .thenReturn(Optional.empty());
        when(snowflake.nextId()).thenReturn(225L); // yields base62 code: "3D"
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        ShortenResponse response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("3D", response.getShortCode());
        verify(urlRepository, times(1)).save(any(Url.class));
        verify(valueOperations, times(1)).set(eq("3D"), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Redirect info from Redis Cache should bypass MongoDB completely")
    void redirectInfoCacheHit() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Mock a cache payload representing cached JSON
        String cachedJson = "{\"url\":\"https://google.com\",\"isOneTime\":false}";
        when(valueOperations.get("abc")).thenReturn(cachedJson);

        RedirectResponse response = urlService.getRedirectInfo("abc");

        assertNotNull(response);
        assertTrue(response.getRedirect());
        assertEquals("https://google.com", response.getUrl());
        
        verify(urlRepository, never()).findByShortCode(anyString());
    }

    @Test
    @DisplayName("Redirecting an expired URL must trigger database cleanup and throw ResourceNotFoundException")
    void redirectExpiredUrl() {
        Url expiredUrl = Url.builder()
                .shortCode("abc")
                .originalUrl("https://google.com")
                .expiresAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("abc")).thenReturn(null);
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(expiredUrl));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                () -> urlService.getRedirectInfo("abc"));

        assertEquals("This shortened link has expired.", exception.getMessage());
        assertTrue(exception.isExpired());
        
        verify(urlRepository, times(1)).delete(expiredUrl);
        verify(stringRedisTemplate, times(1)).delete("abc");
    }

    @Test
    @DisplayName("Checking password protection metadata should return requiresPassword and not reveal long target URL")
    void redirectPasswordRequired() {
        Url protectedUrl = Url.builder()
                .shortCode("abc")
                .originalUrl("https://google.com")
                .password("hashed_pwd")
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("abc")).thenReturn(null);
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(protectedUrl));

        RedirectResponse response = urlService.getRedirectInfo("abc");

        assertNotNull(response);
        assertTrue(response.getRequiresPassword());
        assertEquals("abc", response.getShortCode());
        assertNull(response.getUrl()); // Long URL remains secure!
    }

    @Test
    @DisplayName("Verifying correct password should delete attempts and return original destination URL")
    void verifyPasswordSuccess() {
        Url protectedUrl = Url.builder()
                .shortCode("abc")
                .originalUrl("https://google.com")
                .password("hashed_pwd")
                .isOneTime(false)
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(protectedUrl));
        when(passwordEncoder.matches("my_password", "hashed_pwd")).thenReturn(true);

        VerifyPasswordResponse response = urlService.verifyPassword("abc", "my_password", "127.0.0.1");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("https://google.com", response.getOriginalUrl());
        
        verify(stringRedisTemplate, times(1)).delete("pw_attempts:abc:127.0.0.1");
        verify(urlRepository, times(1)).save(protectedUrl); // Saves click updates
    }

    @Test
    @DisplayName("Verifying wrong password must throw UNAUTHORIZED exception")
    void verifyPasswordFail() {
        Url protectedUrl = Url.builder()
                .shortCode("abc")
                .originalUrl("https://google.com")
                .password("hashed_pwd")
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(protectedUrl));
        when(passwordEncoder.matches("wrong_password", "hashed_pwd")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> urlService.verifyPassword("abc", "wrong_password", "127.0.0.1"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("The password you entered is incorrect.", exception.getReason());
    }

    @Test
    @DisplayName("Exceeding 5 password attempts in one minute must throw RateLimitExceededException")
    void verifyPasswordRateLimitExceeded() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("pw_attempts:abc:127.0.0.1")).thenReturn(6L);

        assertThrows(RateLimitExceededException.class, 
                () -> urlService.verifyPassword("abc", "any_password", "127.0.0.1"));

        verify(urlRepository, never()).findByShortCode(anyString());
    }
}
