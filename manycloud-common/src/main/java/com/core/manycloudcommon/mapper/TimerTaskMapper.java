package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.TimerTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TimerTaskMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TimerTask record);

    int insertSelective(TimerTask record);

    TimerTask selectByTaskNoType(@Param("taskNo") String taskNo, @Param("type") Integer type);

    List<TimerTask> selectPendingByType(@Param("type") Integer type);

    TimerTask selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TimerTask record);

    int updateByPrimaryKey(TimerTask record);
}