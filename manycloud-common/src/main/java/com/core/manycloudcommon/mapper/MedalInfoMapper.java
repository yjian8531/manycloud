package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.MedalInfo;

import java.util.List;

public interface MedalInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(MedalInfo record);

    int insertSelective(MedalInfo record);

    MedalInfo selectByPrimaryKey(Integer id);

    List<MedalInfo> selectAll();

    int updateByPrimaryKeySelective(MedalInfo record);

    int updateByPrimaryKey(MedalInfo record);
}