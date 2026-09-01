package com.vote.system.controller;

import com.vote.system.common.Result;
import com.vote.system.common.UserContext;
import com.vote.system.dto.VoteCreateDTO;
import com.vote.system.entity.Vote;
import com.vote.system.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

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
    public Result<?> getVote(@PathVariable Long id,
                             @RequestHeader(value = "Authorization", required = false) String auth) {
        String openid = UserContext.getOpenid();
        return Result.ok(voteService.getVoteVO(id, openid));
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
