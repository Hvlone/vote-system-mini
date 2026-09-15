package com.vote.system.controller;

import com.vote.system.common.Result;
import com.vote.system.common.UserContext;
import com.vote.system.dto.PageVO;
import com.vote.system.dto.StatsVO;
import com.vote.system.entity.VoteRecord;
import com.vote.system.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/votes/{id}/stats")
    public Result<StatsVO> stats(@PathVariable Long id) {
        return Result.ok(statsService.getStats(id, UserContext.getUserId()));
    }

    @GetMapping("/votes/{id}/details")
    public Result<List<VoteRecord>> details(@PathVariable Long id,
                                             @RequestParam(defaultValue = "1") int page) {
        return Result.ok(statsService.getDetails(id, page, UserContext.getUserId()));
    }

    @GetMapping("/my/records")
    public Result<PageVO<VoteRecord>> myRecords(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return Result.ok(statsService.getMyRecords(UserContext.getOpenid(), page, size));
    }
}
