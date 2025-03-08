package com.ruoyi.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // 其他配置...
            .antMatchers(
                "/login",
                "/captchaImage",
                "/system/sms/code",
                "/system/sms/login"  // 确保这个路径被添加到白名单
            ).permitAll()
            // 其他配置...
        ;
    }
} 