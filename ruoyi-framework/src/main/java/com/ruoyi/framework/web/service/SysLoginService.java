package com.ruoyi.framework.web.service;

import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.user.BlackListException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.exception.user.UserNotExistsException;
import com.ruoyi.common.exception.user.UserPasswordNotMatchException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.security.context.AuthenticationContextHolder;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.framework.web.service.SysSmsService;
import com.ruoyi.common.core.domain.model.SmsLoginBody;
import com.ruoyi.common.exception.user.UserException;
import com.ruoyi.common.constant.UserStatus;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.system.service.ISysSmsService;

/**
 * 登录校验方法
 * 
 * @author ruoyi
 */
@Component
public class SysLoginService {
    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysSmsService smsService;

    @Autowired
    private ISysSmsService sysSmsService;

    @Autowired
    private SysPermissionService permissionService;

    /**
     * 登录验证
     * 
     * @param username 用户名
     * @param password 密码
     * @param code     验证码
     * @param uuid     唯一标识
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid) {
        // 验证码校验
        validateCaptcha(username, code, uuid);
        // 登录前置校验
        loginPreCheck(username, password);
        // 用户验证
        Authentication authentication = null;
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username,
                    password);
            AuthenticationContextHolder.setContext(authenticationToken);
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL,
                        MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            } else {
                AsyncManager.me()
                        .execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new ServiceException(e.getMessage());
            }
        } finally {
            AuthenticationContextHolder.clearContext();
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS,
                MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUserId());
        // 生成token
        return tokenService.createToken(loginUser);
    }

    /**
     * 校验验证码
     * 
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid) {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled) {
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
            String captcha = redisCache.getCacheObject(verifyKey);
            if (captcha == null) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL,
                        MessageUtils.message("user.jcaptcha.expire")));
                throw new CaptchaExpireException();
            }
            redisCache.deleteObject(verifyKey);
            if (!code.equalsIgnoreCase(captcha)) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL,
                        MessageUtils.message("user.jcaptcha.error")));
                throw new CaptchaException();
            }
        }
    }

    /**
     * 登录前置校验
     * 
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password) {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            AsyncManager.me().execute(
                    AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL,
                    MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL,
                    MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr())) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL,
                    MessageUtils.message("login.blocked")));
            throw new BlackListException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(IpUtils.getIpAddr());
        sysUser.setLoginDate(DateUtils.getNowDate());
        userService.updateUserProfile(sysUser);
    }

    /**
     * 短信登录
     *
     * @param mobile  手机号
     * @param smsCode 短信验证码
     * @param uuid    唯一标识
     * @return 结果
     */
    public String smsLogin(String mobile, String smsCode, String uuid) {
        // 验证短信验证码
        if (!sysSmsService.verifySmsCode(uuid, smsCode)) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(mobile, Constants.LOGIN_FAIL,
                    MessageUtils.message("user.jcaptcha.error")));
            throw new CaptchaException();
        }

        // 获取手机号对应的用户
        String redisKey = Constants.SMS_MOBILE_KEY + uuid;
        Object cachedMobile = redisCache.getCacheObject(redisKey);
        if (cachedMobile == null) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(mobile, Constants.LOGIN_FAIL,
                    MessageUtils.message("user.jcaptcha.expire")));
            throw new CaptchaExpireException();
        }

        // 查询用户信息
        SysUser user = userService.selectUserByPhonenumber(mobile);
        if (user == null) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(mobile, Constants.LOGIN_FAIL,
                    MessageUtils.message("user.not.exists")));
            throw new UserNotExistsException();
        }

        if (UserStatus.DELETED.equals(user.getDelFlag())) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(mobile, Constants.LOGIN_FAIL,
                    MessageUtils.message("user.password.delete")));
            throw new UserPasswordNotMatchException();
        }

        if (UserStatus.DISABLE.equals(user.getStatus())) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(mobile, Constants.LOGIN_FAIL,
                    MessageUtils.message("user.blocked")));
            throw new UserPasswordNotMatchException();
        }

        AsyncManager.me().execute(AsyncFactory.recordLogininfor(mobile, Constants.LOGIN_SUCCESS,
                MessageUtils.message("user.login.success")));

        // 生成token并返回
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user,
                permissionService.getMenuPermission(user));
        recordLoginInfo(user.getUserId());
        return tokenService.createToken(loginUser);
    }

    /**
     * 根据手机号加载用户
     */
    public LoginUser loadUserByMobile(String mobile) {
        // 根据手机号查询用户 - 使用正确的方法名
        SysUser user = userService.selectUserByPhonenumber(mobile);
        if (user == null) {
            return null;
        }

        // 创建LoginUser对象
        return createLoginUser(user);
    }

    /**
     * 创建LoginUser对象
     */
    private LoginUser createLoginUser(SysUser user) {
        return new LoginUser(user.getUserId(), user.getDeptId(), user, null);
    }
}
