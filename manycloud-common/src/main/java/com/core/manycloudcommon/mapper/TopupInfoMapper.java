package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.TopupInfo;
import org.apache.ibatis.annotations.Param;

public interface TopupInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TopupInfo record);

    int insertSelective(TopupInfo record);

    TopupInfo selectByPrimaryKey(Integer id);

    TopupInfo selectByNo(@Param("topupNo") String topupNo);

    int updateByPrimaryKeySelective(TopupInfo record);

    int updateByPrimaryKey(TopupInfo record);
}