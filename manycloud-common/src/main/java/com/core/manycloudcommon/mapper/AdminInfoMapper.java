package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.AdminInfo;
import org.apache.ibatis.annotations.Param;

public interface AdminInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(AdminInfo record);

    int insertSelective(AdminInfo record);

    AdminInfo selectByPrimaryKey(Integer id);

    AdminInfo selectByAccount(@Param("account") String account);

    int updateByPrimaryKeySelective(AdminInfo record);

    int updateByPrimaryKey(AdminInfo record);
}