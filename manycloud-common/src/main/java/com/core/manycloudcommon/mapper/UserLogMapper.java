package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.UserLog;

import java.util.List;
import java.util.Map;

public interface UserLogMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(UserLog record);

    int insertSelective(UserLog record);

    UserLog selectByPrimaryKey(Integer id);

    List<UserLog> selectList(Map<String,Object> param);

    int updateByPrimaryKeySelective(UserLog record);

    int updateByPrimaryKeyWithBLOBs(UserLog record);

    int updateByPrimaryKey(UserLog record);
}