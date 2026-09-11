package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.UserIdentity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserIdentityMapper {

    @Select("SELECT id, user_id AS userId, provider, open_id AS openId, nickname FROM user_identity WHERE provider = #{provider} AND open_id = #{openId} LIMIT 1")
    UserIdentity findByProviderOpenId(@Param("provider") String provider, @Param("openId") String openId);

    @Insert("INSERT INTO user_identity(user_id, provider, open_id, nickname) VALUES(#{userId}, #{provider}, #{openId}, #{nickname})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserIdentity row);
}
