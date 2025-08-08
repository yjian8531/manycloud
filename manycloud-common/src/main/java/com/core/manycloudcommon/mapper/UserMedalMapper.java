package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.UserMedal;
import org.apache.ibatis.annotations.Param;

public interface UserMedalMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(UserMedal record);

    int insertSelective(UserMedal record);

    UserMedal selectByPrimaryKey(Integer id);

    UserMedal selectByUserId(@Param("userId") String userId);

    int updateByPrimaryKeySelective(UserMedal record);

    int updateByPrimaryKey(UserMedal record);
}