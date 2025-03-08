package com.ruoyi.common.constant;

/**
 * 用户状态
 * 
 * @author ruoyi
 */
public class UserStatus
{
    /**
     * 正常
     */
    public static final String NORMAL = "0";
    
    /**
     * 停用
     */
    public static final String DISABLE = "1";
    
    /**
     * 删除
     */
    public static final String DELETED = "2";
    
    /**
     * 获取状态码
     */
    public String getCode()
    {
        return this.toString();
    }
} 