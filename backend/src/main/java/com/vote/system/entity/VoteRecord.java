package com.vote.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VoteRecord {
    private Long id;
    private String openid;
    private Long voteId;
    private String answerContent;
    private String ipAddress;
    private LocalDateTime submittedAt;
    /** 冗余展示字段：投票标题（非表列，由接口层回填，便于前端列表直接展示） */
    private String voteTitle;
}
