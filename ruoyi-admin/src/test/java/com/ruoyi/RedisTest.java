package com.ruoyi;

import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RedisTest {

    @Autowired
    private RedisCache redisCache;

    @Test
    public void testRedisCacheOperations() {
        String testKey = "test_key";
        String testValue = "test_value";

        // Store value
        redisCache.setCacheObject(testKey, testValue);
        System.out.println("Stored value in Redis");

        // Retrieve value
        String retrievedValue = redisCache.getCacheObject(testKey);
        System.out.println("Retrieved value: " + retrievedValue);

        // Delete value
        redisCache.deleteObject(testKey);
        System.out.println("Deleted value from Redis");

        // Verify deletion
        String afterDeletion = redisCache.getCacheObject(testKey);
        System.out.println("After deletion: " + afterDeletion);
    }
}