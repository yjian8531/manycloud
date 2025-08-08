package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.SysParam;
import org.apache.ibatis.annotations.Param;

public interface SysParamMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(SysParam record);

    int insertSelective(SysParam record);

    SysParam selectByPrimaryKey(Integer id);

    SysParam selectByTail(@Param("tail") String tail);

    int updateByPrimaryKeySelective(SysParam record);

    int updateByPrimaryKey(SysParam record);
}