package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.PlatformAccount;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PlatformAccountMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(PlatformAccount record);

    int insertSelective(PlatformAccount record);

    PlatformAccount selectByPrimaryKey(Integer id);

    PlatformAccount selectDefault(@Param("label") String label);

    List<PlatformAccount> selectList(@Param("label") String label,@Param("account") String account);

    int updateByPrimaryKeySelective(PlatformAccount record);

    int updateByPrimaryKey(PlatformAccount record);

    int updateDefaultByPlatform(@Param("label") String label);
}