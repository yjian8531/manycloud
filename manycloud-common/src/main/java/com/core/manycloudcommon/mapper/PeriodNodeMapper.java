package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.PeriodNode;

public interface PeriodNodeMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(PeriodNode record);

    int insertSelective(PeriodNode record);

    PeriodNode selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(PeriodNode record);

    int updateByPrimaryKey(PeriodNode record);
}