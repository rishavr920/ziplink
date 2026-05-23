package com.ziplink.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Configuration class to enable MongoDB auditing (createdAt, updatedAt).
 * Keeping this out of the main ZiplinkApplication class prevents slicing tests (like @WebMvcTest)
 * from failing due to missing MongoDB beans in the context.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
