package com.vote.system.controller;

import com.vote.system.common.Result;
import com.vote.system.dto.LoginDTO;
import com.vote.system.dto.LoginVO;
import com.vote.system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录。
     * 无 logout 端点：JWT 无状态，服务端无法作废已签发 token，
     * 退出登录由前端清理本地 token/storage 完成（见 miniapp 我的页）。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }
}
