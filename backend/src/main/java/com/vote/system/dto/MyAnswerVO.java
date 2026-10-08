package com.vote.system.dto;

import lombok.Data;

/** 单题作答（#13）：questionId + answer（单选=选项ID；多选=ID数组；填空=文本） */
@Data
public class MyAnswerVO {
    private Long questionId;
    private Object answer;
}
