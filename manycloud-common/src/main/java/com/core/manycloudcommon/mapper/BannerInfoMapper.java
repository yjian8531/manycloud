package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.BannerInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BannerInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(BannerInfo record);

    int insertSelective(BannerInfo record);

    BannerInfo selectByPrimaryKey(Integer id);

    List<BannerInfo> selectByType(@Param("type") Integer type);

    List<BannerInfo> selectAll();

    int updateByPrimaryKeySelective(BannerInfo record);

    int updateByPrimaryKey(BannerInfo record);
}