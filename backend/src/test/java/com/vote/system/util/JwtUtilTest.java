package com.vote.system.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil(
            "vote-system-jwt-secret-key-please-change-in-prod-2026", 2);

    @Test
    void generateAndParse_success() {
        String token = jwtUtil.generate(1L, "openid_test", "ADMIN");
        Claims claims = jwtUtil.parse(token);
        assertEquals("1", claims.getSubject());
        assertEquals("openid_test", claims.get("openid"));
        assertEquals("ADMIN", claims.get("role"));
    }

    @Test
    void parse_tamperedToken_throws() {
        String token = jwtUtil.generate(1L, "openid_test", "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThrows(JwtException.class, () -> jwtUtil.parse(tampered));
    }

    @Test
    void parse_expiredToken_throws() {
        JwtUtil expired = new JwtUtil(
                "vote-system-jwt-secret-key-please-change-in-prod-2026", 0);
        String token = expired.generate(1L, "openid_test", "USER");
        assertThrows(JwtException.class, () -> expired.parse(token));
    }
}
