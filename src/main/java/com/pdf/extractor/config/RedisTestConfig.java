package com.pdf.extractor.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisTestConfig implements CommandLineRunner {

	@Autowired
	private StringRedisTemplate redisTemplate;

	/**
	 * This bean is used to test the Redis connection at application startup.
	 * It attempts to set and get a value from Redis to verify connectivity.
	 */
	
	@Override
	public void run(String... args) throws Exception {
		try {
			redisTemplate.opsForValue().set("test-key", "connected");
			String value = redisTemplate.opsForValue().get("test-key");
			System.out.println("✅ Redis Test Value: " + value);
		} catch (Exception e) {
			System.err.println("❌ Redis connection failed: " + e.getMessage());
		}
	}
}
