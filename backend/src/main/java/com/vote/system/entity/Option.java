package com.vote.system.entity;

import lombok.Data;

@Data
public class Option {
    private Long id;
    private Long questionId;
    private String optionText;
    private Integer sortOrder;
}
