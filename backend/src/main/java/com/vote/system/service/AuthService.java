package com.vote.system.service;

import com.vote.system.common.BusinessException;
import com.vote.system.dto.LoginDTO;
import com.vote.system.dto.LoginVO;
import com.vote.system.entity.User;
import com.vote.system.mapper.UserMapper;
import com.vote.system.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public LoginVO login(LoginDTO dto) {
        String openid = resolveOpenid(dto.getCode());

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

    /**
     * 解析 openid
     * 开发模式：code 以 "test_" 开头，直接取后段当 openid（不调微信接口）
     * 生产模式：调微信 code2session 接口换 openid
     */
    private String resolveOpenid(String code) {
        if (code == null || code.isBlank()) {
            throw BusinessException.badRequest("code不能为空");
        }
        if (code.startsWith("test_")) {
            return code.substring(5);
        }
        // 生产模式：取消下方注释并配置 appId/secret
        // String url = "https://api.weixin.qq.com/sns/jscode2session?appid=APP_ID&secret=APP_SECRET&js_code=" + code + "&grant_type=authorization_code";
        // JSONObject resp = RestTemplate.getForObject(url, JSONObject.class);
        // return resp.getString("openid");
        return code;
    }
}
