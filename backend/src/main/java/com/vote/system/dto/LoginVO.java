package com.vote.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private String token;
    private Long userId;
    private String openid;
    private String role;
    private String nickname;
}
