package com.vote.system.controller;

import com.vote.system.common.Result;
import com.vote.system.common.UserContext;
import com.vote.system.dto.SubmitDTO;
import com.vote.system.dto.VoteVO;
import com.vote.system.service.SubmitService;
import com.vote.system.util.IpUtil;
import com.vote.system.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
public class SubmitController {

    private final SubmitService submitService;
    private final JwtUtil jwtUtil;

    /**
     * 公开浏览：未登录也能查看活动详情。
     * 该路径被拦截器整体放行，故此处对 Authorization 做"可选"解析：
     * 携带有效 token 时解析出 openid 用于返回 hasVoted，缺失/无效 token 不影响匿名浏览。
     */
    @GetMapping("/{id}/visible")
    public Result<VoteVO> getVisible(@PathVariable Long id,
                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        String openid = parseOpenidIfPresent(authorization);
        return Result.ok(submitService.getVisible(id, openid));
    }

    private String parseOpenidIfPresent(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            Claims claims = jwtUtil.parse(authorization.substring(7));
            return claims.get("openid", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            // token 无效/过期仅影响 hasVoted 展示，不影响公开访问
            return null;
        }
    }

    /**
     * 提交投票：需登录（拦截器已保证 UserContext 有值）
     */
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id,
                                @Valid @RequestBody SubmitDTO dto,
                                HttpServletRequest request) {
        String ip = IpUtil.getIp(request);
        submitService.submit(id, dto, UserContext.getOpenid(), ip);
        return Result.ok();
    }
}
