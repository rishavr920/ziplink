package com.ziplink.repository;

import com.ziplink.model.Url;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UrlRepository interface.
 * 
 * Provides database abstraction for the "urls" collection. Spring Boot automatically
 * instantiates a concrete proxy of this class during compile-time/startup.
 */
@Repository
public interface UrlRepository extends MongoRepository<Url, String> {

    /**
     * Finds a URL mapping by its unique Base62 short code.
     * Used during link redirection flows.
     * 
     * @param shortCode The encoded short code.
     * @return Optional containing the found Url document, or empty.
     */
    Optional<Url> findByShortCode(String shortCode);

    /**
     * Custom finder method to check for duplicate standard mappings.
     * Matches Mongoose: Url.findOne({ originalUrl: longUrl, isOneTime: false, password: null })
     * 
     * @param originalUrl The long target URL.
     * @param isOneTime Whether it's a one-time link.
     * @param password The hashed password value (should be null for standard deduplication).
     * @return Optional of the existing Url entity.
     */
    Optional<Url> findByOriginalUrlAndIsOneTimeAndPassword(String originalUrl, boolean isOneTime, String password);
}
