package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.MedalCommission;
import org.apache.ibatis.annotations.Param;

public interface MedalCommissionMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(MedalCommission record);

    int insertSelective(MedalCommission record);

    MedalCommission selectByPrimaryKey(Integer id);

    MedalCommission selectByRatio(@Param("medalId") Integer medalId, @Param("levelId")Integer levelId);

    int updateByPrimaryKeySelective(MedalCommission record);

    int updateByPrimaryKey(MedalCommission record);
}