package com.pdf.extractor.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisTestConfig {

	@Bean
    public CommandLineRunner testRedisConnection(StringRedisTemplate redisTemplate) {
        return args -> {
            try {
                redisTemplate.opsForValue().set("test-key", "connected");
                String value = redisTemplate.opsForValue().get("test-key");
                System.out.println("✅ Redis Test Value: " + value);
            } catch (Exception e) {
                System.err.println("❌ Redis connection failed: " + e.getMessage());
            }
        };
    }
}
