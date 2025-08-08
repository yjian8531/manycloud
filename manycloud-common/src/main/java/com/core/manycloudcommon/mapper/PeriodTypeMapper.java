package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.PeriodType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PeriodTypeMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(PeriodType record);

    int insertSelective(PeriodType record);

    PeriodType selectByPrimaryKey(Integer id);

    List<PeriodType> selectByNodeId(@Param("nodeId") Integer nodeId);

    int updateByPrimaryKeySelective(PeriodType record);

    int updateByPrimaryKey(PeriodType record);
}