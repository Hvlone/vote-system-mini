package com.vote.system.mapper;

import com.vote.system.entity.VoteRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VoteRecordMapper {

    @Insert("INSERT INTO vote_record (openid, vote_id, answer_content, ip_address) " +
            "VALUES (#{openid}, #{voteId}, #{answerContent}, #{ipAddress})")
    int insert(VoteRecord record);

    @Select("SELECT * FROM vote_record WHERE openid = #{openid} AND vote_id = #{voteId}")
    VoteRecord selectByOpenidAndVoteId(@Param("openid") String openid, @Param("voteId") Long voteId);

    @Select("SELECT COUNT(*) FROM vote_record WHERE vote_id = #{voteId}")
    long countByVoteId(Long voteId);

    @Select("SELECT * FROM vote_record WHERE vote_id = #{voteId} ORDER BY submitted_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<VoteRecord> selectByVoteId(@Param("voteId") Long voteId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM vote_record WHERE openid = #{openid} ORDER BY submitted_at DESC")
    List<VoteRecord> selectByOpenid(String openid);

    @Delete("DELETE FROM vote_record WHERE vote_id = #{voteId}")
    int deleteByVoteId(Long voteId);

    @Select("SELECT COUNT(*) FROM vote_record WHERE vote_id = #{voteId} " +
            "AND ip_address = #{ip} AND submitted_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)")
    long countRecentByVoteIdAndIp(@Param("voteId") Long voteId,
                                  @Param("ip") String ip,
                                  @Param("minutes") int minutes);
}
