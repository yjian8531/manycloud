package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.SysLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysLogMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(SysLog record);

    int insertSelective(SysLog record);

    SysLog selectByPrimaryKey(Integer id);

    List<SysLog> selectByUser(@Param("userId") String userId,@Param("status")Integer status);

    Integer selectUnreadByUser(@Param("userId") String userId);

    int updateByPrimaryKeySelective(SysLog record);

    int updateByPrimaryKeyWithBLOBs(SysLog record);

    int updateByPrimaryKey(SysLog record);
}