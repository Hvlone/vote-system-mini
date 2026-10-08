package com.vote.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** 修改截止时间入参（仅发布中的投票可用） */
@Data
public class VoteEndTimeDTO {
    @NotNull(message = "截止时间不能为空")
    private LocalDateTime endTime;
}
