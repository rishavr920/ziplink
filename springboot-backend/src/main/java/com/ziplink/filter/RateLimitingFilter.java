package com.ziplink.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ziplink.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * RateLimitingFilter.
 * 
 * Uses a Redis counter schema to limit each unique client IP to 100 API calls per 15 minutes.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_MINUTES = 15;
    private static final String REDIS_PREFIX = "rate_limit:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    public RateLimitingFilter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIP(request);
        String redisKey = REDIS_PREFIX + ip;

        try {
            Long currentCount = stringRedisTemplate.opsForValue().increment(redisKey);

            if (currentCount == null) {
                log.error("Failed to query Redis rate limiting for IP: {}", ip);
                filterChain.doFilter(request, response);
                return;
            }

            if (currentCount == 1L) {
                stringRedisTemplate.expire(redisKey, WINDOW_MINUTES, TimeUnit.MINUTES);
                log.debug("Initialized rate limiting window for IP: {} (TTL: {} min)", ip, WINDOW_MINUTES);
            }

            if (currentCount > MAX_REQUESTS) {
                log.warn("Rate limit exceeded for IP: {} (requests: {})", ip, currentCount);
                writeErrorResponse(response);
                return;
            }

            log.debug("Rate limit status for IP {}: {}/{}", ip, currentCount, MAX_REQUESTS);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Redis error in RateLimitingFilter: {}. Permitting traffic.", e.getMessage());
            filterChain.doFilter(request, response);
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private void writeErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429); // 429 Too Many Requests
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .error("Too many requests")
                .message("Too many requests, please try again later.")
                .build();

        String json = objectMapper.writeValueAsString(error);
        response.getWriter().write(json);
    }
}
