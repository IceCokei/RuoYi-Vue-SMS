package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisCache;

@RestController
@RequestMapping("/system/redis")
public class RedisTestController {

    @Autowired
    private RedisCache redisCache;

    @GetMapping("/test")
    public AjaxResult testRedis() {
        try {
            // Test writing to Redis
            String testKey = "test_" + System.currentTimeMillis();
            String testValue = "Hello Redis at " + System.currentTimeMillis();
            
            redisCache.setCacheObject(testKey, testValue);
            String retrieved = redisCache.getCacheObject(testKey);
            
            boolean success = testValue.equals(retrieved);
            
            // Clean up
            redisCache.deleteObject(testKey);
            
            if (success) {
                return AjaxResult.success("Redis连接正常")
                        .put("testKey", testKey)
                        .put("testValue", testValue)
                        .put("retrieved", retrieved);
            } else {
                return AjaxResult.error("Redis写入成功但读取结果不匹配")
                        .put("testKey", testKey)
                        .put("testValue", testValue)
                        .put("retrieved", retrieved);
            }
        } catch (Exception e) {
            return AjaxResult.error("Redis连接测试失败: " + e.getMessage());
        }
    }
} 