package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.constant.Constants;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/system/sms/debug")
public class SmsDebugController {

    @Autowired
    private RedisCache redisCache;

    @GetMapping("/check")
    public AjaxResult checkSmsCode(@RequestParam String mobile) {
        Map<String, Object> result = new HashMap<>();
        
        // Check all possible key formats
        String directKey = Constants.SMS_CODE_KEY + mobile;
        String directValue = redisCache.getCacheObject(directKey);
        
        String alternateKey = "sms_code:" + mobile;
        String alternateValue = redisCache.getCacheObject(alternateKey);
        
        result.put("directKey", directKey);
        result.put("directValue", directValue);
        result.put("alternateKey", alternateKey);
        result.put("alternateValue", alternateValue);
        
        return AjaxResult.success()
                .put("results", result);
    }
} 