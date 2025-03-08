package com.ruoyi.web.model;

/**
 * 短信登录对象
 * 
 * @author ruoyi
 */
public class SmsLoginBody
{
    /**
     * 手机号
     */
    private String mobile;

    /**
     * 短信验证码
     */
    private String smsCode;

    /**
     * 唯一标识
     */
    private String uuid;

    public String getMobile()
    {
        return mobile;
    }

    public void setMobile(String mobile)
    {
        this.mobile = mobile;
    }

    public String getSmsCode()
    {
        return smsCode;
    }

    public void setSmsCode(String smsCode)
    {
        this.smsCode = smsCode;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }
} 