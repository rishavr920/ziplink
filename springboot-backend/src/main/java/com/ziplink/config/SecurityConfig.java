package com.ziplink.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * SecurityConfig.
 * 
 * Provides cryptographic utilities.
 * We register a BCryptPasswordEncoder bean to hash and check passwords for protected links.
 */
@Configuration
public class SecurityConfig {

    /**
     * Registers a BCryptPasswordEncoder bean.
     * Replaces the 'bcryptjs' Node library.
     * 
     * BCrypt automatically handles generation of random salts and embeds them within
     * the hashed output, making it highly secure against brute-force attacks.
     * 
     * Strength parameter 10 matches the Node.js implementation: bcrypt.hash(password, 10).
     * 
     * @return BCryptPasswordEncoder
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
