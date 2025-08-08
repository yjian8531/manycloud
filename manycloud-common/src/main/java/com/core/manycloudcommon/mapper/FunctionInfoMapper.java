package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.FunctionInfo;

import java.util.List;

public interface FunctionInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(FunctionInfo record);

    int insertSelective(FunctionInfo record);

    FunctionInfo selectByPrimaryKey(Integer id);

    List<FunctionInfo> selectAll();

    int updateByPrimaryKeySelective(FunctionInfo record);

    int updateByPrimaryKey(FunctionInfo record);
}