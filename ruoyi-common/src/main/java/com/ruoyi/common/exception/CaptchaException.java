package com.ruoyi.common.exception;

/**
 * 验证码错误异常类
 * 
 * @author ruoyi
 */
public class CaptchaException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public CaptchaException()
    {
    }

    public CaptchaException(String message)
    {
        super(message);
    }
} 