package com.vote.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class StatsVO {
    private Long totalVotes;
    private List<QuestionStats> questions;

    @Data
    public static class QuestionStats {
        private Long questionId;
        private String questionText;
        private Integer questionType;
        private List<OptionStats> options;
        private Long textAnswerCount;
    }

    @Data
    public static class OptionStats {
        private Long optionId;
        private String optionText;
        private Long count;
        private Double percentage;
    }
}
