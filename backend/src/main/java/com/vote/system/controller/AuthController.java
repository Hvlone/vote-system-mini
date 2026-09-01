package com.vote.system.controller;

import com.vote.system.common.Result;
import com.vote.system.common.UserContext;
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

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        UserContext.clear();
        return Result.ok();
    }
}
