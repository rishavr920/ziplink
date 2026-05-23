package com.ziplink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziplink.dto.*;
import com.ziplink.service.UrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    // Mock Redis templates to prevent RateLimitingFilter from failing during slice startup
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/shorten with valid inputs should return 201 Created")
    void shortenUrlSuccess() throws Exception {
        ShortenRequest request = new ShortenRequest("https://google.com", null, false, null);
        ShortenResponse response = ShortenResponse.builder()
                .originalUrl("https://google.com")
                .shortUrl("http://localhost/abc")
                .shortCode("abc")
                .isOneTime(false)
                .isPasswordProtected(false)
                .build();

        when(urlService.shortenUrl(any(ShortenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/abc"))
                .andExpect(jsonPath("$.isPasswordProtected").value(false));
    }

    @Test
    @DisplayName("POST /api/shorten with invalid URL must throw 400 Bad Request and list validation details")
    void shortenUrlValidationFail() throws Exception {
        // Missing http/https protocol
        ShortenRequest request = new ShortenRequest("ftp://google.com", "123", false, null);

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]").value("Please provide a valid URL starting with http:// or https://"));
    }

    @Test
    @DisplayName("GET /api/redirect/{code} should retrieve redirect metadata")
    void getRedirectInfoSuccess() throws Exception {
        RedirectResponse response = RedirectResponse.builder()
                .redirect(true)
                .url("https://google.com")
                .build();

        when(urlService.getRedirectInfo("abc")).thenReturn(response);

        mockMvc.perform(get("/api/redirect/abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect").value(true))
                .andExpect(jsonPath("$.url").value("https://google.com"));
    }

    @Test
    @DisplayName("GET /{code} direct lookup should issue HTTP 302 redirect")
    void directRedirectSuccess() throws Exception {
        when(urlService.resolveDirectRedirect("abc")).thenReturn("https://google.com");

        mockMvc.perform(get("/abc"))
                .andExpect(status().isFound()) // 302 Found
                .andExpect(header().string(HttpHeaders.LOCATION, "https://google.com"));
    }

    @Test
    @DisplayName("GET /{code} for password protected link must yield 403 Forbidden with warning JSON")
    void directRedirectPasswordForbidden() throws Exception {
        when(urlService.resolveDirectRedirect("abc"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "This link requires a password to access."));

        mockMvc.perform(get("/abc"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Password protected link"))
                .andExpect(jsonPath("$.requiresPassword").value(true))
                .andExpect(jsonPath("$.shortCode").value("abc"));
    }
}
