package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.framework.web.service.SysLoginService;

import java.util.HashSet;
import java.util.Map;

@RestController
public class SmsLoginController {

    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysLoginService loginService;

    @PostMapping("/sms/simple-login")
    public AjaxResult smsLogin(@RequestBody Map<String, String> loginBody) {
        String mobile = loginBody.get("mobile");
        String code = loginBody.get("smsCode");

        // 基本验证
        if (StringUtils.isEmpty(mobile) || StringUtils.isEmpty(code)) {
            return AjaxResult.error("手机号或验证码不能为空");
        }

        // 验证短信验证码逻辑
        // 这里省略验证码验证，实际项目应该增加验证逻辑

        // 尝试加载用户
        LoginUser loginUser = loginService.loadUserByMobile(mobile);

        // 如果用户不存在，自动创建用户
        if (loginUser == null) {
            // 创建新用户
            SysUser newUser = new SysUser();
            newUser.setPhonenumber(mobile);
            newUser.setUserName("mobile_" + mobile); // 生成用户名
            newUser.setNickName("手机用户" + mobile.substring(mobile.length() - 4)); // 生成昵称

            // 设置默认密码，建议生成随机密码
            String defaultPassword = SecurityUtils.encryptPassword("123456");
            newUser.setPassword(defaultPassword);

            // 设置其他必要字段
            newUser.setStatus("0"); // 正常状态
            newUser.setDelFlag("0"); // 未删除

            // 保存用户
            try {
                userService.insertUser(newUser);
                System.out.println("自动创建用户成功：" + mobile);

                // 分配默认角色（通常是common角色）
                Long[] roleIds = { 2L }; // 假设2是普通用户角色ID
                userService.insertUserAuth(newUser.getUserId(), roleIds);

                // 重新加载用户信息
                loginUser = loginService.loadUserByMobile(mobile);

                // 确保权限不为null
                if (loginUser != null && loginUser.getPermissions() == null) {
                    loginUser.setPermissions(new HashSet<>());
                }
            } catch (Exception e) {
                return AjaxResult.error("自动创建用户失败：" + e.getMessage());
            }
        }

        // 此时用户应该存在
        if (loginUser == null) {
            return AjaxResult.error("系统错误，无法创建或加载用户");
        }

        // 确保权限不为null（防止NullPointerException）
        if (loginUser.getPermissions() == null) {
            loginUser.setPermissions(new HashSet<>());
        }

        // 生成token
        String token = tokenService.createToken(loginUser);

        return AjaxResult.success("登录成功").put("token", token);
    }
}