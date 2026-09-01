package com.vote.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vote.system.common.BusinessException;
import com.vote.system.dto.StatsVO;
import com.vote.system.entity.*;
import com.vote.system.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final VoteMapper voteMapper;
    private final QuestionMapper questionMapper;
    private final OptionMapper optionMapper;
    private final VoteRecordMapper voteRecordMapper;
    private final ObjectMapper objectMapper;

    private static final int PAGE_SIZE = 20;

    /**
     * 统计：总数 + 各题选项占比
     */
    public StatsVO getStats(Long voteId) {
        Vote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            throw BusinessException.notFound("投票活动不存在");
        }

        long total = voteRecordMapper.countByVoteId(voteId);
        List<Question> questions = questionMapper.selectByVoteId(voteId);
        List<VoteRecord> records = voteRecordMapper.selectByVoteId(voteId, 0, 10000);

        StatsVO vo = new StatsVO();
        vo.setTotalVotes(total);

        List<StatsVO.QuestionStats> qStatsList = new ArrayList<>();
        for (Question q : questions) {
            StatsVO.QuestionStats qs = new StatsVO.QuestionStats();
            qs.setQuestionId(q.getId());
            qs.setQuestionText(q.getQuestionText());
            qs.setQuestionType(q.getQuestionType());

            List<Option> options = optionMapper.selectByQuestionId(q.getId());

            if (q.getQuestionType() == 3) {
                // 填空题：统计回答数
                long textCount = 0;
                for (VoteRecord r : records) {
                    List<Map<String, Object>> answers = parseAnswers(r.getAnswerContent());
                    for (Map<String, Object> a : answers) {
                        if (q.getId().equals(toLong(a.get("questionId")))) {
                            Object ans = a.get("answer");
                            if (ans != null && !ans.toString().isBlank()) {
                                textCount++;
                            }
                        }
                    }
                }
                qs.setTextAnswerCount(textCount);
                qs.setOptions(Collections.emptyList());
            } else {
                // 单选/多选：统计各选项被选次数
                Map<Long, Long> optionCounts = new HashMap<>();
                for (Option o : options) {
                    optionCounts.put(o.getId(), 0L);
                }
                for (VoteRecord r : records) {
                    List<Map<String, Object>> answers = parseAnswers(r.getAnswerContent());
                    for (Map<String, Object> a : answers) {
                        if (q.getId().equals(toLong(a.get("questionId")))) {
                            Object ans = a.get("answer");
                            if (ans instanceof List<?> list) {
                                for (Object o : list) {
                                    Long optId = toLong(o);
                                    optionCounts.merge(optId, 1L, Long::sum);
                                }
                            } else if (ans != null) {
                                Long optId = toLong(ans);
                                optionCounts.merge(optId, 1L, Long::sum);
                            }
                        }
                    }
                }
                List<StatsVO.OptionStats> osList = new ArrayList<>();
                for (Option o : options) {
                    StatsVO.OptionStats os = new StatsVO.OptionStats();
                    os.setOptionId(o.getId());
                    os.setOptionText(o.getOptionText());
                    long count = optionCounts.getOrDefault(o.getId(), 0L);
                    os.setCount(count);
                    os.setPercentage(total > 0 ? Math.round(count * 10000.0 / total) / 100.0 : 0.0);
                    osList.add(os);
                }
                qs.setOptions(osList);
                qs.setTextAnswerCount(null);
            }
            qStatsList.add(qs);
        }
        vo.setQuestions(qStatsList);
        return vo;
    }

    /**
     * 明细列表（分页）
     */
    public List<VoteRecord> getDetails(Long voteId, int page) {
        int offset = (page - 1) * PAGE_SIZE;
        return voteRecordMapper.selectByVoteId(voteId, offset, PAGE_SIZE);
    }

    /**
     * 我的投票记录
     */
    public List<VoteRecord> getMyRecords(String openid) {
        return voteRecordMapper.selectByOpenid(openid);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAnswers(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析答案JSON失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.valueOf(o.toString()); } catch (Exception e) { return null; }
    }
}
