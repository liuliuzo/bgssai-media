package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.SysUser;
import com.bgssai.media.common.domain.SysUserExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper {
    @org.apache.ibatis.annotations.Select("SELECT current_session_id FROM sys_user WHERE id = #{id} AND status = 'active' AND role_code = #{role}")
    String currentSession(@Param("id") Long id, @Param("role") String role);
    @org.apache.ibatis.annotations.Update("UPDATE sys_user SET current_session_id = #{sid} WHERE id = #{id} AND status = 'active' AND role_code = #{role}")
    int bindSession(@Param("id") Long id, @Param("role") String role, @Param("sid") String sid);

    long countByExample(SysUserExample example);
    List<SysUser> selectByExample(SysUserExample example);
    SysUser selectByPrimaryKey(Long id);
    int insertSelective(SysUser row);
    int updateByPrimaryKeySelective(SysUser row);
}
