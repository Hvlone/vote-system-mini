package com.vote.system.controller;

import com.vote.system.common.Result;
import com.vote.system.common.UserContext;
import com.vote.system.dto.MyAnswerVO;
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

    /** 我的作答（#13）：仅当前登录用户自己的作答；无作答记录返回 404（视为未参与） */
    @GetMapping("/votes/{id}/my-record")
    public Result<List<MyAnswerVO>> myRecord(@PathVariable Long id) {
        return Result.ok(statsService.getMyRecord(id, UserContext.getOpenid()));
    }
}
