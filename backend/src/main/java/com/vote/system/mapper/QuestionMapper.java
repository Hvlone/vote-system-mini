package com.vote.system.mapper;

import com.vote.system.entity.Question;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QuestionMapper {

    @Insert("INSERT INTO question (vote_id, question_text, question_type, is_required, sort_order) " +
            "VALUES (#{voteId}, #{questionText}, #{questionType}, #{isRequired}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Question question);

    @Select("SELECT * FROM question WHERE vote_id = #{voteId} ORDER BY sort_order")
    List<Question> selectByVoteId(Long voteId);

    @Delete("DELETE FROM question WHERE vote_id = #{voteId}")
    int deleteByVoteId(Long voteId);
}
