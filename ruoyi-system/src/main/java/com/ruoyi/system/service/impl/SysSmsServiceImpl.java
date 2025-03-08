package com.ruoyi.system.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.net.URLEncoder;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.service.ISysSmsService;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import com.ruoyi.common.utils.StringUtils;

@Service
public class SysSmsServiceImpl implements ISysSmsService {
    private static final Logger log = LoggerFactory.getLogger(SysSmsServiceImpl.class);

    @Resource
    private RedisCache redisCache;

    @Value("${sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${sms.provider:smsbao}")
    private String smsProvider;

    @Value("${sms.username}")
    private String smsUsername;

    @Value("${sms.password}")
    private String smsPassword;

    @Value("${sms.sign:若依}")
    private String smsSign;

    @Value("${sms.template:【%s】您的验证码是%s，%d分钟内有效。若非本人操作请忽略此消息。}")
    private String smsTemplate;

    @Value("${sms.test-mode:false}")
    private boolean testMode;

    private static final String SMS_API_URL = "https://api.smsbao.com/sms";

    /**
     * 发送短信验证码
     *
     * @param mobile 手机号
     * @return 结果
     */
    @Override
    public String sendSmsCode(String mobile) {
        // 生成6位随机验证码
        String code = generateSmsCode(6);

        try {
            // 格式化短信内容
            String content = String.format("【%s】%s", smsSign, String.format(smsTemplate, code));
            log.info("准备发送短信验证码 手机号: {}, 验证码: {}, 内容: {}", mobile, code, content);

            // 测试模式下直接返回成功，不实际调用API
            if (testMode) {
                log.info("测试模式下，模拟短信发送成功，验证码: {}", code);
                String uuid = storeVerificationCodeInRedis(mobile, code);
                return uuid;
            }

            // 实际发送短信
            if (smsEnabled) {
                try {
                    String result = sendSms(mobile, content);
                    log.info("短信发送结果: {}", result);

                    // 检查短信发送结果
                    if ("0".equals(result)) {
                        // 验证码存入Redis，有效期5分钟
                        String uuid = storeVerificationCodeInRedis(mobile, code);
                        return uuid;
                    } else {
                        String errorMsg;
                        switch (result) {
                            case "30":
                                errorMsg = "短信平台密码错误";
                                break;
                            case "40":
                                errorMsg = "短信平台账号不存在";
                                break;
                            case "41":
                                errorMsg = "短信余额不足";
                                break;
                            case "43":
                                errorMsg = "IP地址受限";
                                break;
                            case "50":
                                errorMsg = "内容含有敏感词";
                                break;
                            case "51":
                                errorMsg = "手机号码格式错误";
                                break;
                            default:
                                errorMsg = "未知错误码: " + result;
                        }
                        log.error("短信发送失败: {}", errorMsg);
                        return null;
                    }
                } catch (Exception e) {
                    log.error("短信API调用异常", e);
                    return null;
                }
            } else {
                log.info("短信功能已禁用，模拟发送成功");
                // 即使短信禁用也保存验证码以便测试
                String uuid = storeVerificationCodeInRedis(mobile, code);
                return uuid;
            }
        } catch (Exception e) {
            log.error("发送短信验证码异常", e);
            return null;
        }
    }

    /**
     * 将验证码存入Redis
     */
    private String storeVerificationCodeInRedis(String mobile, String code) {
        // 生成唯一标识
        String uuid = generateUuid();
        String verifyKey = Constants.SMS_CODE_KEY + uuid;

        // 将手机号和验证码存入Redis
        redisCache.setCacheObject(verifyKey, code, Constants.SMS_EXPIRATION, TimeUnit.MINUTES);
        redisCache.setCacheObject(Constants.SMS_MOBILE_KEY + uuid, mobile, Constants.SMS_EXPIRATION, TimeUnit.MINUTES);

        return uuid;
    }

    /**
     * 发送短信
     */
    private String sendSms(String mobile, String content) throws Exception {
        log.info("准备发送短信：手机号=[{}]，内容=[{}]", mobile, content);

        try {
            // 确保手机号格式正确
            String formattedMobile = mobile.trim().replaceAll("\\s+", "");

            // 构建URL
            String encodedContent = URLEncoder.encode(content, "UTF-8");
            String apiUrl = String.format("%s?u=%s&p=%s&m=%s&c=%s",
                    SMS_API_URL,
                    smsUsername,
                    smsPassword,
                    formattedMobile,
                    encodedContent);

            log.info("短信API调用完整URL: {}", apiUrl);

            // 使用HttpURLConnection发送请求
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // 在sendSms方法的HttpURLConnection创建后添加
            if (System.getProperty("http.proxyHost") != null) {
                String proxyHost = System.getProperty("http.proxyHost");
                String proxyPort = System.getProperty("http.proxyPort");
                log.info("使用代理: {}:{}", proxyHost, proxyPort);
            }

            // 发送请求并获取响应代码
            int responseCode = connection.getResponseCode();
            log.info("HTTP响应代码: {}", responseCode);

            if (responseCode != 200) {
                log.error("HTTP请求失败，状态码: {}", responseCode);
                return "-2";
            }

            // 读取响应内容
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            }

            String result = response.toString().trim();
            log.info("短信API响应: [{}]", result);

            return result;
        } catch (Exception e) {
            log.error("发送短信异常", e);
            return "-1";
        }
    }

    /**
     * 生成随机验证码
     */
    private String generateSmsCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成UUID
     */
    private String generateUuid() {
        return java.util.UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * MD5加密
     */
    private String md5(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte[] digest = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                int i = b & 0xff;
                if (i < 16) {
                    sb.append("0");
                }
                sb.append(Integer.toHexString(i));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("MD5加密异常", e);
            return "";
        }
    }

    @Override
    public boolean verifySmsCode(String uuid, String code) {
        if (StringUtils.isEmpty(uuid) || StringUtils.isEmpty(code)) {
            return false;
        }

        String verifyKey = Constants.SMS_CODE_KEY + uuid;
        String captcha = redisCache.getCacheObject(verifyKey);

        if (captcha == null) {
            return false;
        }

        // 验证完成后删除验证码
        redisCache.deleteObject(verifyKey);
        redisCache.deleteObject(Constants.SMS_MOBILE_KEY + uuid);

        return code.equalsIgnoreCase(captcha);
    }
}