package com.vote.system.mapper;

import com.vote.system.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE openid = #{openid}")
    User selectByOpenid(String openid);

    @Insert("INSERT INTO user (openid, nickname, avatar_url, role) " +
            "VALUES (#{openid}, #{nickname}, #{avatarUrl}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
