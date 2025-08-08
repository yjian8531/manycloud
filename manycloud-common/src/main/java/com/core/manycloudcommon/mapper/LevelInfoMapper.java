package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.LevelInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LevelInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(LevelInfo record);

    int insertSelective(LevelInfo record);

    List<LevelInfo> selectAll();

    LevelInfo selectByPrimaryKey(Integer id);

    LevelInfo selectByLevel(@Param("level") Integer level);

    LevelInfo selectByUser(@Param("userId") String userId);

    int updateByPrimaryKeySelective(LevelInfo record);

    int updateByPrimaryKey(LevelInfo record);
}