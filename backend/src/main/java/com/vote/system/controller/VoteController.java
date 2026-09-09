package com.vote.system.controller;

import com.vote.system.common.Result;
import com.vote.system.common.UserContext;
import com.vote.system.dto.VoteCreateDTO;
import com.vote.system.dto.VoteVO;
import com.vote.system.entity.Vote;
import com.vote.system.service.VoteService;
import com.vote.system.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody VoteCreateDTO dto) {
        Long voteId = voteService.create(dto, UserContext.getUserId());
        return Result.ok(voteId);
    }

    @GetMapping("/list")
    public Result<List<Vote>> listMyVotes() {
        return Result.ok(voteService.listMyVotes(UserContext.getUserId()));
    }

    @GetMapping("/{id}")
    public Result<?> getVote(@PathVariable Long id) {
        return Result.ok(voteService.getVoteVO(id, UserContext.getUserId(), UserContext.getOpenid()));
    }

    /**
     * 公开详情（免登录浏览）。该路径被拦截器整体放行，
     * 故此处对 Authorization 做"可选"解析：有效 token 时返回 hasVoted，缺失/无效则匿名放行。
     */
    @GetMapping("/{id}/visible")
    public Result<VoteVO> getVisible(@PathVariable Long id,
                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        String openid = parseOpenidIfPresent(authorization);
        return Result.ok(voteService.getVisibleDetail(id, openid));
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

    @PutMapping("/{id}")
    public Result<Void> edit(@PathVariable Long id, @Valid @RequestBody VoteCreateDTO dto) {
        voteService.edit(id, dto, UserContext.getUserId());
        return Result.ok();
    }

    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        voteService.publish(id, UserContext.getUserId());
        return Result.ok();
    }

    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id) {
        voteService.stop(id, UserContext.getUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        voteService.delete(id, UserContext.getUserId());
        return Result.ok();
    }
}
