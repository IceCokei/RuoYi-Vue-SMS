package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.SmsLoginBody;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.SysSmsService;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.system.service.ISysSmsService;

/**
 * 短信验证码控制器
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/sms")
public class SysSmsController extends BaseController {

    @Autowired
    private ISysSmsService sysSmsService;

    @Autowired
    private SysLoginService loginService;

    /**
     * 获取短信验证码
     */
    @GetMapping("/code")
    public AjaxResult getSmsCode(String mobile) {
        // 校验手机号是否为空
        if (mobile == null || mobile.isEmpty()) {
            return AjaxResult.error("手机号不能为空");
        }

        // 校验手机号格式
        if (!mobile.matches("^1[3-9]\\d{9}$")) {
            return AjaxResult.error("手机号格式不正确");
        }

        // 格式化手机号，去除空格
        mobile = mobile.trim().replaceAll("\\s+", "");

        // 发送短信验证码
        try {
            String uuid = sysSmsService.sendSmsCode(mobile);

            if (uuid != null) {
                return AjaxResult.success("验证码发送成功").put("uuid", uuid);
            } else {
                return AjaxResult.error("验证码发送失败，请稍后重试");
            }
        } catch (Exception e) {
            logger.error("短信发送异常", e);
            return AjaxResult.error("验证码发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/code")
    public AjaxResult sendSmsCode(@RequestBody SmsLoginBody smsLoginBody) {
        // 校验手机号是否为空
        if (smsLoginBody.getMobile() == null || smsLoginBody.getMobile().isEmpty()) {
            return AjaxResult.error("手机号不能为空");
        }

        // 校验手机号格式
        if (!smsLoginBody.getMobile().matches("^1[3-9]\\d{9}$")) {
            return AjaxResult.error("手机号格式不正确");
        }

        // 格式化手机号，去除空格
        String mobile = smsLoginBody.getMobile().trim().replaceAll("\\s+", "");

        try {
            // 调用发送短信服务
            String uuid = sysSmsService.sendSmsCode(mobile);

            if (uuid != null) {
                return AjaxResult.success("验证码发送成功").put("uuid", uuid);
            } else {
                return AjaxResult.error("验证码发送失败，请稍后重试");
            }
        } catch (Exception e) {
            logger.error("短信发送异常", e);
            return AjaxResult.error("验证码发送失败: " + e.getMessage());
        }
    }

    /**
     * 短信登录
     */
    @PostMapping("/login")
    public AjaxResult smsLogin(@RequestBody SmsLoginBody smsLoginBody) {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.smsLogin(smsLoginBody.getMobile(), smsLoginBody.getSmsCode(),
                smsLoginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }
}