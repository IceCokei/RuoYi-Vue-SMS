package com.ruoyi.framework.web.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.common.exception.CaptchaExpireException;
import com.ruoyi.common.exception.CaptchaException;

/**
 * 短信服务
 * 
 * @author ruoyi
 */
@Component
public class SysSmsService {
    private static final Logger log = LoggerFactory.getLogger(SysSmsService.class);

    @Autowired
    private RedisCache redisCache;

    @Value("${sms.username}")
    private String smsUsername;

    @Value("${sms.password}")
    private String smsPassword;

    @Value("${sms.sign}")
    private String smsSign;

    /**
     * 发送短信验证码
     * 
     * @param mobile 手机号
     * @return 结果
     */
    public AjaxResult sendSmsCode(String mobile) {
        if (StringUtils.isEmpty(mobile)) {
            return AjaxResult.error("手机号不能为空");
        }

        // 生成6位随机验证码
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

        // 保存验证码信息
        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.SMS_CODE_KEY + mobile;

        // 将验证码存入Redis，有效期5分钟
        redisCache.setCacheObject(verifyKey, code, 5, TimeUnit.MINUTES);

        // 短信内容
        String content = "【" + smsSign + "】您的验证码是" + code + ",５分钟内有效。若非本人操作请忽略此消息。";

        // 调用短信API发送短信
        boolean sendResult = sendSms(mobile, content);

        if (sendResult) {
            AjaxResult ajax = AjaxResult.success("短信发送成功");
            ajax.put("uuid", uuid);
            return ajax;
        } else {
            return AjaxResult.error("短信发送失败，请稍后重试");
        }
    }

    /**
     * 验证短信验证码
     *
     * @param mobile  手机号
     * @param smsCode 短信验证码
     * @param uuid    唯一标识
     */
    public void validateSmsCode(String mobile, String smsCode, String uuid) {
        String verifyKey = CacheConstants.SMS_CODE_KEY + mobile;
        String captcha = redisCache.getCacheObject(verifyKey);

        if (captcha == null) {
            throw new CaptchaExpireException();
        }

        redisCache.deleteObject(verifyKey);

        if (!smsCode.equalsIgnoreCase(captcha)) {
            throw new CaptchaException();
        }
    }

    /**
     * 发送短信
     * 
     * @param phone   手机号
     * @param content 短信内容
     * @return 发送结果
     */
    private boolean sendSms(String phone, String content) {
        String httpUrl = "http://api.smsbao.com/sms";

        StringBuffer httpArg = new StringBuffer();
        httpArg.append("u=").append(smsUsername).append("&");
        httpArg.append("p=").append(md5(smsPassword)).append("&");
        httpArg.append("m=").append(phone).append("&");
        httpArg.append("c=").append(encodeUrlString(content, "UTF-8"));

        String result = request(httpUrl, httpArg.toString());
        log.info("SMS send result: {}", result);

        // 根据短信宝API，返回0表示发送成功
        return "0".equals(result);
    }

    /**
     * 发送HTTP请求
     */
    private String request(String httpUrl, String httpArg) {
        BufferedReader reader = null;
        String result = null;
        StringBuffer sbf = new StringBuffer();
        httpUrl = httpUrl + "?" + httpArg;

        try {
            URL url = new URL(httpUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            InputStream is = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String strRead = reader.readLine();
            if (strRead != null) {
                sbf.append(strRead);
                while ((strRead = reader.readLine()) != null) {
                    sbf.append("\n");
                    sbf.append(strRead);
                }
            }
            reader.close();
            result = sbf.toString();
        } catch (Exception e) {
            log.error("Send SMS error", e);
            return "-1";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    log.error("Close reader error", e);
                }
            }
        }
        return result;
    }

    /**
     * MD5加密
     */
    private String md5(String plainText) {
        StringBuffer buf = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte b[] = md.digest();
            int i;
            buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 encryption error", e);
        }
        return buf.toString();
    }

    /**
     * URL编码
     */
    private String encodeUrlString(String str, String charset) {
        String strret = null;
        if (str == null)
            return str;
        try {
            strret = java.net.URLEncoder.encode(str, charset);
        } catch (Exception e) {
            log.error("URL encode error", e);
            return null;
        }
        return strret;
    }
}