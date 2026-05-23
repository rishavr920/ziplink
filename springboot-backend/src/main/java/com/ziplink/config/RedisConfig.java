package com.ziplink.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisConfig.
 * 
 * Configures the Spring Data Redis connection templates.
 * We specify String serializations for Redis keys to ensure they are human-readable
 * (i.e. not prefixing garbage characters that Java's default JdkSerialization causes),
 * matching how keys look when written by Node.js.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Exposes a RedisTemplate with String keys and JSON values for generic caching operations.
     * 
     * @param connectionFactory The auto-configured lettuce/jedis connection factory.
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use StringSerializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use Jackson2 JSON serializer for values to store structured JSON strings
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
