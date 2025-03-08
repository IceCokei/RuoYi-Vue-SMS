package com.ruoyi.system.service;

/**
 * 短信服务接口
 * 
 * @author ruoyi
 */
public interface ISysSmsService
{
    /**
     * 发送短信验证码
     * 
     * @param mobile 手机号
     * @return 生成的验证码唯一标识UUID，失败返回null
     */
    public String sendSmsCode(String mobile);
    
    /**
     * 验证短信验证码
     * 
     * @param uuid 验证码唯一标识
     * @param code 验证码
     * @return 是否验证成功
     */
    public boolean verifySmsCode(String uuid, String code);
} 