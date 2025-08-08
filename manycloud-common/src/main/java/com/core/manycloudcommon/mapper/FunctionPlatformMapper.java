package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.FunctionPlatform;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FunctionPlatformMapper {
    int deleteByPrimaryKey(Integer id);

    int deleteByIds(@Param("list")List<Integer> list);

    int insert(FunctionPlatform record);

    int insertSelective(FunctionPlatform record);

    int insertList(@Param("list")List<FunctionPlatform> list);

    FunctionPlatform selectByPrimaryKey(Integer id);

    List<FunctionPlatform> selectByPlatform(@Param("platformId") Integer platformId);

    int updateByPrimaryKeySelective(FunctionPlatform record);

    int updateByPrimaryKey(FunctionPlatform record);
}