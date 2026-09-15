package com.vote.system.util;

import com.vote.system.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 微信登录工具类：用 wx.login 拿到的 code 换取 openid。
 *
 * 两条路径，互斥：
 *  - 调试登录：code 以 "test_" 开头时，直接取后段作为 openid（不调微信）。
 *    由 wechat.debug-login-enabled 控制，仅供本地联调；生产必须在 application-prod.yml
 *    或环境变量中置为 false。
 *  - 正式路径：调用微信 code2session 接口换取 openid。
 *
 * 注意：这里不存在「非 test_ 前缀即原样当作 openid」的兜底——那种写法会让任意 code
 * 直接换到合法登录态，等于绕过微信鉴权。
 */
@Slf4j
@Component
public class WechatUtil {

    /** code2session 接口地址，{} 为 URI 变量占位符，由 RestClient 做替换与编码 */
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session"
            + "?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    /** 调试登录 code 前缀 */
    private static final String DEBUG_CODE_PREFIX = "test_";

    private final String appid;
    private final String secret;
    private final boolean debugLoginEnabled;
    private final RestClient restClient;

    public WechatUtil(@Value("${wechat.appid:}") String appid,
                      @Value("${wechat.secret:}") String secret,
                      @Value("${wechat.debug-login-enabled:false}") boolean debugLoginEnabled,
                      RestClient.Builder restClientBuilder) {
        this.appid = appid;
        this.secret = secret;
        this.debugLoginEnabled = debugLoginEnabled;
        // 连接 3s / 读取 5s：微信接口不可用时快速失败，避免登录请求长时间挂起
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    /**
     * 用登录 code 换取 openid
     * @param code wx.login 返回的 code；调试登录时为 test_ 前缀的自造 code
     * @return openid
     */
    public String resolveOpenid(String code) {
        if (code == null || code.isBlank()) {
            throw BusinessException.badRequest("code不能为空");
        }
        if (debugLoginEnabled && code.startsWith(DEBUG_CODE_PREFIX)) {
            log.warn("调试登录生效(wechat.debug-login-enabled=true)，直接用 code 后段作为 openid: {}", code);
            return code.substring(DEBUG_CODE_PREFIX.length());
        }
        return code2session(code);
    }

    /** 调用微信 code2session 接口，用 code 换取 openid */
    private String code2session(String code) {
        if (appid == null || appid.isBlank() || secret == null || secret.isBlank()) {
            throw new BusinessException(500, "服务端未配置微信登录参数(wechat.appid / wechat.secret)");
        }
        Code2SessionResp resp;
        try {
            resp = restClient.get()
                    .uri(CODE2SESSION_URL, appid, secret, code)
                    .retrieve()
                    .body(Code2SessionResp.class);
        } catch (Exception e) {
            log.error("调用微信 code2session 失败: {}", e.getMessage());
            throw new BusinessException(500, "微信登录服务暂不可用，请稍后重试");
        }
        if (resp == null) {
            throw new BusinessException(500, "微信登录失败: 接口无响应");
        }
        // 微信约定：成功时不返回 errcode，失败时返回非 0 的 errcode
        if (resp.errcode() != null && resp.errcode() != 0) {
            log.warn("微信 code2session 返回错误: errcode={}, errmsg={}", resp.errcode(), resp.errmsg());
            throw BusinessException.unauthorized("微信登录失败: "
                    + (resp.errmsg() != null ? resp.errmsg() : resp.errcode()));
        }
        if (resp.openid() == null || resp.openid().isBlank()) {
            throw new BusinessException(500, "微信登录失败: 未返回 openid");
        }
        return resp.openid();
    }

    /** code2session 响应体：只声明用到的字段，其余由 Jackson 忽略 */
    private record Code2SessionResp(String openid, Integer errcode, String errmsg) {
    }
}
