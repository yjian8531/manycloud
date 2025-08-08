package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.SysOperation;
import org.apache.ibatis.annotations.Param;

public interface SysOperationMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(SysOperation record);

    int insertSelective(SysOperation record);

    SysOperation selectByPrimaryKey(Integer id);

    SysOperation selectByTail(@Param("tail") String tail);

    int updateByPrimaryKeySelective(SysOperation record);

    int updateByPrimaryKey(SysOperation record);
}