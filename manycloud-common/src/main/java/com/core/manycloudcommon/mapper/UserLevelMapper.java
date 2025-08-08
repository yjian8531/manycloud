package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.UserLevel;
import org.apache.ibatis.annotations.Param;

public interface UserLevelMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(UserLevel record);

    int insertSelective(UserLevel record);

    UserLevel selectByPrimaryKey(Integer id);

    UserLevel selectByUser(@Param("userId") String userId);

    int updateByPrimaryKeySelective(UserLevel record);

    int updateByPrimaryKey(UserLevel record);
}