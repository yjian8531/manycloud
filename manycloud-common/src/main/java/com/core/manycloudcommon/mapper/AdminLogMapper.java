package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.AdminLog;

import java.util.List;
import java.util.Map;

public interface AdminLogMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(AdminLog record);

    int insertSelective(AdminLog record);

    AdminLog selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(AdminLog record);

    int updateByPrimaryKeyWithBLOBs(AdminLog record);

    int updateByPrimaryKey(AdminLog record);

    List<AdminLog> selectList(Map<String, Object> map);
}