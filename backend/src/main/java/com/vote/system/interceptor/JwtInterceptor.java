package com.vote.system.interceptor;

import com.vote.system.common.UserContext;
import com.vote.system.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器：校验 Authorization: Bearer <token>，将用户信息放入 UserContext
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行预检请求
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(response, "未登录或登录已过期");
        }
        try {
            Claims claims = jwtUtil.parse(auth.substring(7));
            UserContext.set(Long.valueOf(claims.getSubject()),
                    claims.get("openid", String.class),
                    claims.get("role", String.class));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(response, "Token无效或已过期");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
