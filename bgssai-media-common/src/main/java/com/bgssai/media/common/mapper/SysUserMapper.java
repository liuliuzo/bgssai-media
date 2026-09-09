package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.SysUser;
import com.bgssai.media.common.domain.SysUserExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper {
    long countByExample(SysUserExample example);
    List<SysUser> selectByExample(SysUserExample example);
    SysUser selectByPrimaryKey(Long id);
    int insertSelective(SysUser row);
    int updateByPrimaryKeySelective(SysUser row);
}
