package com.vote.system.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 工具类：从请求头/远程地址中获取真实客户端 IP
 */
public class IpUtil {

    private IpUtil() {
    }

    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalid(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isInvalid(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private static boolean isInvalid(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }
}
