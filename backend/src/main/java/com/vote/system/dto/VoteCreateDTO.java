package com.vote.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VoteCreateDTO {
    @NotBlank(message = "标题不能为空")
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @NotEmpty(message = "至少添加一道题目")
    @Valid
    private List<QuestionDTO> questions;

    @Data
    public static class QuestionDTO {
        @NotBlank(message = "题目内容不能为空")
        private String questionText;
        private Integer questionType; // 1单选 2多选 3填空
        private Integer isRequired;
        private Integer sortOrder;
        private List<OptionDTO> options; // 填空题可为空
    }

    @Data
    public static class OptionDTO {
        @NotBlank(message = "选项内容不能为空")
        private String optionText;
        private Integer sortOrder;
    }
}
