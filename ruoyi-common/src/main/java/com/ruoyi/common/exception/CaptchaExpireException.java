package com.ruoyi.common.exception;

/**
 * 验证码失效异常类
 * 
 * @author ruoyi
 */
public class CaptchaExpireException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CaptchaExpireException() {
    }

    public CaptchaExpireException(String message) {
        super(message);
    }
}