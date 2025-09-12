package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.AdminInfo;
import com.core.manycloudcommon.entity.PowerBinding;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(AdminInfo record);

    int insertSelective(AdminInfo record);

    AdminInfo selectByPrimaryKey(Integer id);

    AdminInfo selectByAccount(@Param("account") String account);

    int updateByPrimaryKeySelective(AdminInfo record);

    int updateByPrimaryKey(AdminInfo record);

    /** 查询管理员权限绑定（判断是否为超级管理员） */
    List<PowerBinding> selectGroupingByAdmin(@Param("adminId") String adminId);
}