package com.vote.system.service;

import com.vote.system.common.BusinessException;
import com.vote.system.dto.VoteCreateDTO;
import com.vote.system.dto.VoteVO;
import com.vote.system.entity.*;
import com.vote.system.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteMapper voteMapper;
    private final QuestionMapper questionMapper;
    private final OptionMapper optionMapper;
    private final VoteRecordMapper voteRecordMapper;

    @Transactional
    public Long create(VoteCreateDTO dto, Long creatorId) {
        validateQuestions(dto.getQuestions());
        Vote vote = new Vote();
        vote.setTitle(dto.getTitle());
        vote.setDescription(dto.getDescription());
        LocalDateTime start = dto.getStartTime() != null ? dto.getStartTime() : LocalDateTime.now();
        LocalDateTime end = dto.getEndTime() != null ? dto.getEndTime() : LocalDateTime.now().plusDays(7);
        validateTimeRange(start, end);
        vote.setStartTime(start);
        vote.setEndTime(end);
        vote.setStatus(0); // 创建为草稿，需 publish 发布后才可被填写
        vote.setCreatorId(creatorId);
        voteMapper.insert(vote);

        for (VoteCreateDTO.QuestionDTO qDto : dto.getQuestions()) {
            Question q = new Question();
            q.setVoteId(vote.getId());
            q.setQuestionText(qDto.getQuestionText());
            q.setQuestionType(qDto.getQuestionType());
            q.setIsRequired(qDto.getIsRequired() != null ? qDto.getIsRequired() : 1);
            q.setSortOrder(qDto.getSortOrder() != null ? qDto.getSortOrder() : 0);
            questionMapper.insert(q);

            if (qDto.getOptions() != null && !qDto.getOptions().isEmpty()) {
                for (VoteCreateDTO.OptionDTO oDto : qDto.getOptions()) {
                    Option o = new Option();
                    o.setQuestionId(q.getId());
                    o.setOptionText(oDto.getOptionText());
                    o.setSortOrder(oDto.getSortOrder() != null ? oDto.getSortOrder() : 0);
                    optionMapper.insert(o);
                }
            }
        }
        return vote.getId();
    }

    public List<Vote> listMyVotes(Long creatorId) {
        List<Vote> votes = voteMapper.selectByCreator(creatorId);
        // 到期不改 status(状态机单向)，此处推导 expired 供前端直接展示"已结束/截止"
        for (Vote v : votes) {
            v.setExpired(v.getEndTime() != null && LocalDateTime.now().isAfter(v.getEndTime()));
        }
        return votes;
    }

    public VoteVO getVoteVO(Long voteId, Long userId, String openid) {
        Vote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            throw BusinessException.notFound("投票活动不存在");
        }
        // 草稿仅创建者本人可见
        if (vote.getStatus() == 0 && !vote.getCreatorId().equals(userId)) {
            throw BusinessException.forbidden("投票尚未发布");
        }
        return buildDetail(vote, openid);
    }

    /**
     * 公开详情（免登录浏览，见 GET /votes/{id}/visible）：
     * 与 getVoteVO 共用 buildDetail，仅草稿对外一律视为不存在。
     */
    public VoteVO getVisibleDetail(Long voteId, String openid) {
        Vote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            throw BusinessException.notFound("投票活动不存在");
        }
        // 草稿未发布，对外不可见（含创建者，创建者应走管理接口 GET /votes/{id}）
        if (vote.getStatus() == 0) {
            throw BusinessException.notFound("投票尚未发布");
        }
        return buildDetail(vote, openid);
    }

    /**
     * 详情 VO 公共组装：基础信息 + hasVoted + 题目选项（两处详情接口共用）
     */
    private VoteVO buildDetail(Vote vote, String openid) {
        VoteVO vo = new VoteVO();
        vo.setId(vote.getId());
        vo.setTitle(vote.getTitle());
        vo.setDescription(vote.getDescription());
        vo.setStartTime(vote.getStartTime());
        vo.setEndTime(vote.getEndTime());
        vo.setStatus(vote.getStatus());
        vo.setCreatorId(vote.getCreatorId());
        // 到期不改 status(状态机单向)，推导 expired 供前端展示"已结束"并禁用填写按钮
        vo.setExpired(vote.getEndTime() != null && LocalDateTime.now().isAfter(vote.getEndTime()));

        if (openid != null) {
            VoteRecord record = voteRecordMapper.selectByOpenidAndVoteId(openid, vote.getId());
            vo.setHasVoted(record != null);
        } else {
            vo.setHasVoted(false);
        }

        List<Question> questions = questionMapper.selectByVoteId(vote.getId());
        List<VoteVO.QuestionVO> qvos = new ArrayList<>();
        for (Question q : questions) {
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

    @Transactional
    public void edit(Long voteId, VoteCreateDTO dto, Long userId) {
        Vote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            throw BusinessException.notFound("投票活动不存在");
        }
        if (!vote.getCreatorId().equals(userId)) {
            throw BusinessException.forbidden("无权操作他人投票");
        }
        // 仅草稿可编辑：发布/结束后禁止改动题目，避免统计数据错乱
        if (vote.getStatus() != 0) {
            throw BusinessException.badRequest("仅草稿状态的投票可以编辑");
        }
        // 题型与选项语义校验通过后才重建题目，避免发布无法作答的投票
        validateQuestions(dto.getQuestions());
        // 先删旧题目和选项，再重建
        optionMapper.deleteByVoteId(voteId);
        questionMapper.deleteByVoteId(voteId);
        // 更新基本信息（缺省时间沿用原值），题目随后重建
        LocalDateTime start = dto.getStartTime() != null ? dto.getStartTime() : vote.getStartTime();
        LocalDateTime end = dto.getEndTime() != null ? dto.getEndTime() : vote.getEndTime();
        if (start == null || end == null) {
            throw BusinessException.badRequest("开始时间和结束时间不能为空");
        }
        validateTimeRange(start, end);
        vote.setTitle(dto.getTitle());
        vote.setDescription(dto.getDescription());
        vote.setStartTime(start);
        vote.setEndTime(end);
        voteMapper.updateBaseInfo(vote);
        // 重新插入题目
        for (VoteCreateDTO.QuestionDTO qDto : dto.getQuestions()) {
            Question q = new Question();
            q.setVoteId(voteId);
            q.setQuestionText(qDto.getQuestionText());
            q.setQuestionType(qDto.getQuestionType());
            q.setIsRequired(qDto.getIsRequired() != null ? qDto.getIsRequired() : 1);
            q.setSortOrder(qDto.getSortOrder() != null ? qDto.getSortOrder() : 0);
            questionMapper.insert(q);
            if (qDto.getOptions() != null) {
                for (VoteCreateDTO.OptionDTO oDto : qDto.getOptions()) {
                    Option o = new Option();
                    o.setQuestionId(q.getId());
                    o.setOptionText(oDto.getOptionText());
                    o.setSortOrder(oDto.getSortOrder() != null ? oDto.getSortOrder() : 0);
                    optionMapper.insert(o);
                }
            }
        }
    }

    public void publish(Long voteId, Long userId) {
        Vote vote = checkOwnership(voteId, userId);
        if (vote.getStatus() != 0) {
            throw BusinessException.badRequest("仅草稿状态的投票可以发布");
        }
        voteMapper.updateStatus(voteId, 1);
    }

    public void stop(Long voteId, Long userId) {
        Vote vote = checkOwnership(voteId, userId);
        if (vote.getStatus() != 1) {
            throw BusinessException.badRequest("仅发布中的投票可以停止");
        }
        voteMapper.updateStatus(voteId, 2);
    }

    @Transactional
    public void delete(Long voteId, Long userId) {
        Vote vote = checkOwnership(voteId, userId);
        optionMapper.deleteByVoteId(voteId);
        questionMapper.deleteByVoteId(voteId);
        voteRecordMapper.deleteByVoteId(voteId); // 级联清理投票记录
        voteMapper.deleteById(voteId);
    }

    private Vote checkOwnership(Long voteId, Long userId) {
        Vote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            throw BusinessException.notFound("投票活动不存在");
        }
        if (!vote.getCreatorId().equals(userId)) {
            throw BusinessException.forbidden("无权操作他人投票");
        }
        return vote;
    }

    /**
     * 语义校验：题型必须在 {1单选,2多选,3填空} 内，且单选/多选必须带选项、填空题不应带选项。
     * 与提交侧 validateAnswers 保持一致，防止创建出他人永远无法作答的投票。
     */
    private void validateQuestions(List<VoteCreateDTO.QuestionDTO> questions) {
        if (questions == null || questions.isEmpty()) {
            throw BusinessException.badRequest("至少添加一道题目");
        }
        for (VoteCreateDTO.QuestionDTO q : questions) {
            Integer type = q.getQuestionType();
            if (type == null || (type != 1 && type != 2 && type != 3)) {
                throw BusinessException.badRequest("题目类型非法: " + q.getQuestionText());
            }
            List<VoteCreateDTO.OptionDTO> options = q.getOptions();
            boolean hasOptions = options != null && !options.isEmpty();
            if (type != 3 && !hasOptions) {
                throw BusinessException.badRequest("单选/多选题至少需要一个选项: " + q.getQuestionText());
            }
            if (type == 3 && hasOptions) {
                throw BusinessException.badRequest("填空题不应包含选项: " + q.getQuestionText());
            }
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw BusinessException.badRequest("结束时间必须晚于开始时间");
        }
    }
}
