package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysPermissionService;

import java.util.HashSet;
import java.util.Set;

@RestController
public class SysLoginController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysPermissionService permissionService;

    @GetMapping("getInfo")
    public AjaxResult getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        
        // 添加空值检查
        if (loginUser.getPermissions() == null) {
            loginUser.setPermissions(new HashSet<>());
        }
        
        if (!loginUser.getPermissions().equals(permissions)) {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        return ajax;
    }
} 