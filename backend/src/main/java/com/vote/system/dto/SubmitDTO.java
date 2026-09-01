package com.vote.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SubmitDTO {
    @NotEmpty(message = "答案不能为空")
    private List<Answer> answers;

    @Data
    public static class Answer {
        private Long questionId;
        private Object answer; // 单选:Long 多选:List<Long> 填空:String
    }
}
