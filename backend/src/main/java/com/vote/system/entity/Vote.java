package com.vote.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Vote {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Long creatorId;
    private LocalDateTime createdAt;
    /** 是否已过结束时间（推导字段，非表列；到期不改 status，由该字段供前端展示"已结束/截止"） */
    private Boolean expired;
}
