package com.vote.system.controller;

import com.vote.system.common.Result;
import com.vote.system.common.UserContext;
import com.vote.system.dto.SubmitDTO;
import com.vote.system.service.SubmitService;
import com.vote.system.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
public class SubmitController {

    private final SubmitService submitService;
    private final IpUtil ipUtil;

    /**
     * 提交投票：需登录（拦截器已保证 UserContext 有值）
     */
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id,
                                @Valid @RequestBody SubmitDTO dto,
                                HttpServletRequest request) {
        String ip = ipUtil.getIp(request);
        submitService.submit(id, dto, UserContext.getOpenid(), ip);
        return Result.ok();
    }
}
