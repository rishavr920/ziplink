package com.ziplink.controller;

import com.ziplink.dto.*;
import com.ziplink.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * UrlController class.
 * 
 * Maps external web requests to core business logic.
 */
@RestController
@RequestMapping
@CrossOrigin
@Tag(name = "ZipLink Core APIs", description = "Endpoints for link shortening, custom password checks, and dynamic redirection resolution.")
public class UrlController {

    private static final Logger log = LoggerFactory.getLogger(UrlController.class);

    @Autowired
    private UrlService urlService;

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Returns a simple health status message.")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("System is Healthy 🚀");
    }

    @PostMapping("/api/shorten")
    @Operation(
            summary = "Create Short Link",
            description = "Compresses any target URL into a Base62 short code. Optionally supports passwords, one-time expirations, and timed expirations.",
            responses = {
                @ApiResponse(responseCode = "201", description = "New URL successfully shortened.",
                        content = @Content(schema = @Schema(implementation = ShortenResponse.class))),
                @ApiResponse(responseCode = "200", description = "Existing standard link returned (deduplication).",
                        content = @Content(schema = @Schema(implementation = ShortenResponse.class))),
                @ApiResponse(responseCode = "400", description = "Bad Request (e.g. invalid URL).",
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request) {
        log.info("Request received to shorten URL: {}", request.getLongUrl());
        ShortenResponse response = urlService.shortenUrl(request);
        
        HttpStatus status = (response.getMessage() != null) ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/api/redirect/{code}")
    @Operation(
            summary = "Get Redirection Info",
            description = "Consulted by the React dynamic router to determine if redirect is possible or password prompt is needed.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Redirection data compiled successfully.",
                        content = @Content(schema = @Schema(implementation = RedirectResponse.class))),
                @ApiResponse(responseCode = "404", description = "Link not found.",
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(responseCode = "410", description = "Link has expired.",
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<RedirectResponse> getRedirectInfo(
            @Parameter(description = "Alphanumeric Base62 short code key", example = "4uYd7p")
            @PathVariable String code) {
        log.info("Request received for redirect info: {}", code);
        RedirectResponse response = urlService.getRedirectInfo(code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/verify-password/{code}")
    @Operation(
            summary = "Unlock Password Protected Link",
            description = "Compares password input against BCrypt hashes. Max 5 failures per minute per IP.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Password correct. Unlocked URL returned.",
                        content = @Content(schema = @Schema(implementation = VerifyPasswordResponse.class))),
                @ApiResponse(responseCode = "401", description = "Unauthorized (Incorrect password).",
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(responseCode = "429", description = "Too Many Requests (brute force throttling).",
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<VerifyPasswordResponse> verifyPassword(
            @PathVariable String code,
            @Valid @RequestBody VerifyPasswordRequest request,
            HttpServletRequest httpServletRequest) {
        
        String ip = getClientIP(httpServletRequest);
        log.info("Password verification attempt for code: {} from IP: {}", code, ip);
        
        VerifyPasswordResponse response = urlService.verifyPassword(code, request.getPassword(), ip);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{code}")
    @Operation(
            summary = "Direct Redirection",
            description = "Forwards browser requests directly to original destination via HTTP 302 Found redirect.",
            responses = {
                @ApiResponse(responseCode = "302", description = "Redirection successful."),
                @ApiResponse(responseCode = "403", description = "Forbidden (Password-protected).",
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(responseCode = "404", description = "Link not found.",
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<?> redirect(@PathVariable String code) {
        log.info("Direct redirect requested for shortCode: {}", code);
        
        try {
            String targetUrl = urlService.resolveDirectRedirect(code);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, targetUrl)
                    .build();
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.info("Direct redirect blocked for code {}: Password required", code);
                ErrorResponse error = ErrorResponse.builder()
                        .error("Password protected link")
                        .message("This link requires a password to access.")
                        .requiresPassword(true)
                        .shortCode(code)
                        .build();
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            throw ex;
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
