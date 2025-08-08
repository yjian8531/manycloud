package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.UserWx;
import org.apache.ibatis.annotations.Param;

public interface UserWxMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(UserWx record);

    int insertSelective(UserWx record);

    UserWx selectByPrimaryKey(Integer id);

    UserWx selectByUserId(@Param("userId") String userId);

    int updateByPrimaryKeySelective(UserWx record);

    int updateByPrimaryKey(UserWx record);
}