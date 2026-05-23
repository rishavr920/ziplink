package com.ziplink.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SwaggerConfig.
 * 
 * Configures OpenAPI metadata definitions for the Springdoc Swagger UI.
 * This describes all of our URL shortener REST endpoints in a highly professional manner.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ZipLink High-Performance URL Shortener API")
                        .version("1.0.0")
                        .description("Distributed, ultra-fast URL shortening microservice powered by Snowflake ID Generation, MongoDB persistent storage, and Redis low-latency caching.")
                        .contact(new Contact()
                                .name("Rishav Raj")
                                .email("rishavr920@gmail.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
