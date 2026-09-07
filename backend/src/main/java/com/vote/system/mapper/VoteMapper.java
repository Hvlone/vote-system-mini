package com.vote.system.mapper;

import com.vote.system.entity.Vote;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VoteMapper {

    @Insert("INSERT INTO vote (title, description, start_time, end_time, status, creator_id) " +
            "VALUES (#{title}, #{description}, #{startTime}, #{endTime}, #{status}, #{creatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Vote vote);

    @Select("SELECT * FROM vote WHERE id = #{id}")
    Vote selectById(Long id);

    @Select("SELECT * FROM vote WHERE creator_id = #{creatorId} ORDER BY created_at DESC")
    List<Vote> selectByCreator(Long creatorId);

    @Update("UPDATE vote SET title = #{title}, description = #{description}, " +
            "start_time = #{startTime}, end_time = #{endTime} WHERE id = #{id}")
    int updateBaseInfo(Vote vote);

    @Update("UPDATE vote SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Delete("DELETE FROM vote WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM vote WHERE id = #{id} AND status = 1 " +
            "AND NOW() BETWEEN start_time AND end_time")
    Vote selectActiveById(Long id);
}
