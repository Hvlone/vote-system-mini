package com.vote.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vote.system.common.BusinessException;
import com.vote.system.dto.SubmitDTO;
import com.vote.system.dto.VoteVO;
import com.vote.system.entity.Option;
import com.vote.system.entity.Question;
import com.vote.system.entity.Vote;
import com.vote.system.entity.VoteRecord;
import com.vote.system.mapper.OptionMapper;
import com.vote.system.mapper.QuestionMapper;
import com.vote.system.mapper.VoteMapper;
import com.vote.system.mapper.VoteRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitService {

    private final VoteMapper voteMapper;
    private final QuestionMapper questionMapper;
    private final OptionMapper optionMapper;
    private final VoteRecordMapper voteRecordMapper;
    private final ObjectMapper objectMapper;

    private static final int IP_WINDOW_MINUTES = 60;
    private static final int IP_MAX_SUBMIT = 3;

    /**
     * 公开浏览：未登录也能看活动详情
     */
    public VoteVO getVisible(Long voteId, String openid) {
        return getVoteVOInternal(voteId, openid);
    }

    /**
     * 提交投票：防重复双保险 + IP限流 + 必填校验
     */
    public void submit(Long voteId, SubmitDTO dto, String openid, String ip) {
        // ① 校验活动状态
        Vote vote = voteMapper.selectActiveById(voteId);
        if (vote == null) {
            throw BusinessException.badRequest("投票未在进行中或已结束");
        }

        // ② 防重复：应用层先查
        VoteRecord existing = voteRecordMapper.selectByOpenidAndVoteId(openid, voteId);
        if (existing != null) {
            throw BusinessException.conflict("已投过该投票，不可重复提交");
        }

        // ③ IP 限流：同 IP 在时间窗口内对同一投票的提交次数超限则拒绝（防刷票）
        long recent = voteRecordMapper.countRecentByVoteIdAndIp(voteId, ip, IP_WINDOW_MINUTES);
        if (recent >= IP_MAX_SUBMIT) {
            throw BusinessException.tooManyRequests("提交过于频繁，请稍后再试");
        }

        // ④ 校验必填题
        List<Question> questions = questionMapper.selectByVoteId(voteId);
        validateAnswers(dto, questions);

        // ⑤ 序列化答案 + 写入记录（唯一索引兜底）
        try {
            String json = objectMapper.writeValueAsString(dto.getAnswers());
            VoteRecord record = new VoteRecord();
            record.setOpenid(openid);
            record.setVoteId(voteId);
            record.setAnswerContent(json);
            record.setIpAddress(ip);
            voteRecordMapper.insert(record);
            log.info("投票提交成功: voteId={}, openid={}", voteId, openid);
        } catch (JsonProcessingException e) {
            log.error("答案序列化失败", e);
            throw BusinessException.badRequest("答案格式错误");
        } catch (Exception e) {
            // 唯一索引冲突（并发场景兜底）
            if (e.getMessage() != null && e.getMessage().contains("uk_user_vote")) {
                throw BusinessException.conflict("已投过该投票，不可重复提交");
            }
            throw e;
        }
    }

    private void validateAnswers(SubmitDTO dto, List<Question> questions) {
        Map<Long, Question> questionMap = new HashMap<>();
        Map<Long, Set<Long>> questionOptions = new HashMap<>();
        for (Question q : questions) {
            questionMap.put(q.getId(), q);
            Set<Long> optIds = new HashSet<>();
            for (Option o : optionMapper.selectByQuestionId(q.getId())) {
                optIds.add(o.getId());
            }
            questionOptions.put(q.getId(), optIds);
        }

        // ① 结构校验：每条答案必须对应本投票真实题目，且答案形态/选项合法
        Set<Long> answeredQuestionIds = new HashSet<>();
        for (SubmitDTO.Answer a : dto.getAnswers()) {
            if (a.getQuestionId() == null) {
                throw BusinessException.badRequest("答案缺少题目ID");
            }
            if (!answeredQuestionIds.add(a.getQuestionId())) {
                throw BusinessException.badRequest("同一题目重复作答");
            }
            Question q = questionMap.get(a.getQuestionId());
            if (q == null) {
                throw BusinessException.badRequest("包含不属于本投票的题目");
            }
            Integer type = q.getQuestionType();
            if (type == null || (type != 1 && type != 2 && type != 3)) {
                throw BusinessException.badRequest("题目类型非法");
            }
            Object ans = a.getAnswer();
            switch (type) {
                case 1 -> { // 单选：单个选项ID
                    Long optId = toLong(ans);
                    if (optId == null || !questionOptions.get(q.getId()).contains(optId)) {
                        throw BusinessException.badRequest("单选题答案无效: " + q.getQuestionText());
                    }
                }
                case 2 -> { // 多选：选项ID数组
                    if (!(ans instanceof List<?> list)) {
                        throw BusinessException.badRequest("多选题答案格式错误: " + q.getQuestionText());
                    }
                    for (Object o : list) {
                        Long optId = toLong(o);
                        if (optId == null || !questionOptions.get(q.getId()).contains(optId)) {
                            throw BusinessException.badRequest("多选题答案包含无效选项: " + q.getQuestionText());
                        }
                    }
                }
                case 3 -> { // 填空：文本
                    if (!(ans instanceof String)) {
                        throw BusinessException.badRequest("填空题答案格式错误: " + q.getQuestionText());
                    }
                }
                default -> throw BusinessException.badRequest("题目类型非法");
            }
        }

        // ② 必填校验
        for (Question q : questions) {
            if (q.getIsRequired() == null || q.getIsRequired() != 1) {
                continue;
            }
            SubmitDTO.Answer answer = dto.getAnswers().stream()
                    .filter(a -> q.getId().equals(a.getQuestionId()))
                    .findFirst()
                    .orElse(null);
            if (answer == null || isEmptyAnswer(answer.getAnswer())) {
                throw BusinessException.badRequest("必填题未作答: " + q.getQuestionText());
            }
        }
    }

    /** 宽松地把 Number/数字字符串转 Long，失败返回 null */
    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s && !s.isBlank()) {
            try {
                return Long.valueOf(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private boolean isEmptyAnswer(Object ans) {
        if (ans == null) return true;
        if (ans instanceof String s) return s.isBlank();
        if (ans instanceof List<?> l) return l.isEmpty();
        return false;
    }

    private VoteVO getVoteVOInternal(Long voteId, String openid) {
        Vote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            throw BusinessException.notFound("投票活动不存在");
        }
        // 草稿未发布，对外不可见（含创建者，创建者应走 /votes/{id} 管理接口）
        if (vote.getStatus() == 0) {
            throw BusinessException.notFound("投票尚未发布");
        }
        VoteVO vo = new VoteVO();
        vo.setId(vote.getId());
        vo.setTitle(vote.getTitle());
        vo.setDescription(vote.getDescription());
        vo.setStartTime(vote.getStartTime());
        vo.setEndTime(vote.getEndTime());
        vo.setStatus(vote.getStatus());
        vo.setCreatorId(vote.getCreatorId());

        if (openid != null) {
            VoteRecord record = voteRecordMapper.selectByOpenidAndVoteId(openid, voteId);
            vo.setHasVoted(record != null);
        } else {
            vo.setHasVoted(false);
        }

        List<Question> qs = questionMapper.selectByVoteId(voteId);
        List<VoteVO.QuestionVO> qvos = new java.util.ArrayList<>();
        for (Question q : qs) {
            VoteVO.QuestionVO qvo = new VoteVO.QuestionVO();
            qvo.setId(q.getId());
            qvo.setQuestionText(q.getQuestionText());
            qvo.setQuestionType(q.getQuestionType());
            qvo.setIsRequired(q.getIsRequired());
            qvo.setSortOrder(q.getSortOrder());
            qvo.setOptions(optionMapper.selectByQuestionId(q.getId()));
            qvos.add(qvo);
        }
        vo.setQuestions(qvos);
        return vo;
    }
}
