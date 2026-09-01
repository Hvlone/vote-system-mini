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
}
