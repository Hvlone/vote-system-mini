package com.vote.system.service;

import com.vote.system.dto.LoginDTO;
import com.vote.system.dto.LoginVO;
import com.vote.system.entity.User;
import com.vote.system.mapper.UserMapper;
import com.vote.system.util.JwtUtil;
import com.vote.system.util.WechatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final WechatUtil wechatUtil;

    public LoginVO login(LoginDTO dto) {
        String openid = wechatUtil.resolveOpenid(dto.getCode());

        User user = userMapper.selectByOpenid(openid);
        if (user == null) {
            // 首次登录，自动注册
            user = new User();
            user.setOpenid(openid);
            user.setNickname(dto.getNickname() != null ? dto.getNickname() : "用户" + openid.substring(Math.max(0, openid.length() - 6)));
            user.setAvatarUrl(dto.getAvatarUrl());
            user.setRole("USER");
            userMapper.insert(user);
            log.info("新用户注册: openid={}, id={}", openid, user.getId());
        } else {
            // 更新昵称/头像（仅更新有值的字段并持久化，避免 null 覆盖旧值）
            boolean changed = false;
            if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
                user.setNickname(dto.getNickname());
                changed = true;
            }
            if (dto.getAvatarUrl() != null) {
                user.setAvatarUrl(dto.getAvatarUrl());
                changed = true;
            }
            if (changed) {
                userMapper.updateProfile(user);
            }
        }

        String token = jwtUtil.generate(user.getId(), user.getOpenid(), user.getRole());
        return new LoginVO(token, user.getId(), user.getOpenid(), user.getRole(), user.getNickname());
    }
}
