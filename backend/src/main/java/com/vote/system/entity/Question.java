package com.vote.system.entity;

import lombok.Data;

@Data
public class Question {
    private Long id;
    private Long voteId;
    private String questionText;
    private Integer questionType;
    private Integer isRequired;
    private Integer sortOrder;
}
