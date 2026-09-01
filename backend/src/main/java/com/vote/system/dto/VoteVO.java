package com.vote.system.dto;

import com.vote.system.entity.Option;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VoteVO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Long creatorId;
    private Boolean hasVoted;
    private List<QuestionVO> questions;

    @Data
    public static class QuestionVO {
        private Long id;
        private String questionText;
        private Integer questionType;
        private Integer isRequired;
        private Integer sortOrder;
        private List<Option> options;
    }
}
