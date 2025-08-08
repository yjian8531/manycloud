package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.PlatformInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PlatformInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(PlatformInfo record);

    int insertSelective(PlatformInfo record);

    PlatformInfo selectByPrimaryKey(Integer id);

    PlatformInfo selectByLabel(@Param("label") String label);

    List<PlatformInfo> selectList(@Param("label") String label,@Param("name") String name);

    List<PlatformInfo> selectAll();

    int updateByPrimaryKeySelective(PlatformInfo record);

    int updateByPrimaryKey(PlatformInfo record);
}