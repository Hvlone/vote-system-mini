package com.vote.system.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * IP 工具类：获取真实客户端 IP。
 * 默认直连部署只取 remoteAddr（此时客户端无法伪造头绕过限流）；
 * 仅当部署在受信反向代理之后，将 ip.trust-proxies 置为 true，才从 X-Forwarded-For / X-Real-IP 取真实 IP。
 */
@Component
public class IpUtil {

    private final boolean trustProxies;

    public IpUtil(@Value("${ip.trust-proxies:false}") boolean trustProxies) {
        this.trustProxies = trustProxies;
    }

    public String getIp(HttpServletRequest request) {
        String ip = trustProxies ? getForwardedIp(request) : request.getRemoteAddr();
        return ip != null && !ip.isEmpty() ? ip : request.getRemoteAddr();
    }

    private String getForwardedIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalid(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private boolean isInvalid(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }
}
