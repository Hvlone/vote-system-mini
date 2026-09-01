package com.vote.system.mapper;

import com.vote.system.entity.Option;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OptionMapper {

    @Insert("INSERT INTO `option` (question_id, option_text, sort_order) " +
            "VALUES (#{questionId}, #{optionText}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Option option);

    @Select("SELECT * FROM `option` WHERE question_id = #{questionId} ORDER BY sort_order")
    List<Option> selectByQuestionId(Long questionId);

    @Delete("DELETE FROM `option` WHERE question_id IN " +
            "(SELECT id FROM question WHERE vote_id = #{voteId})")
    int deleteByVoteId(Long voteId);
}
