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
        Vote vote = new Vote();
        vote.setTitle(dto.getTitle());
        vote.setDescription(dto.getDescription());
        vote.setStartTime(dto.getStartTime() != null ? dto.getStartTime() : LocalDateTime.now());
        vote.setEndTime(dto.getEndTime() != null ? dto.getEndTime() : LocalDateTime.now().plusDays(7));
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
        return voteMapper.selectByCreator(creatorId);
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

        List<Question> questions = questionMapper.selectByVoteId(voteId);
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
        // 先删旧题目和选项，再重建
        optionMapper.deleteByVoteId(voteId);
        questionMapper.deleteByVoteId(voteId);
        // 更新基本信息
        vote.setTitle(dto.getTitle());
        vote.setDescription(dto.getDescription());
        vote.setStartTime(dto.getStartTime());
        vote.setEndTime(dto.getEndTime());
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
}
