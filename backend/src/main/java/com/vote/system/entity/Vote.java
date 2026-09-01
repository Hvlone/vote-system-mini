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
}
