package com.ziplink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ZiplinkApplication bootstrap class.
 * 
 * The @SpringBootApplication annotation activates:
 * 1. @SpringBootConfiguration: Declares this class as a configuration source.
 * 2. @EnableAutoConfiguration: Tells Spring Boot to configure beans based on our pom.xml (e.g. Mongo, Redis).
 * 3. @ComponentScan: Scans the com.ziplink package for controllers, services, and repositories.
 */
@SpringBootApplication
public class ZiplinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZiplinkApplication.class, args);
    }
}
