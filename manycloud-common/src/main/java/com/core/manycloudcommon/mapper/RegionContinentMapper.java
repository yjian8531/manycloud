package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.RegionContinent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RegionContinentMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(RegionContinent record);

    int insertSelective(RegionContinent record);

    RegionContinent selectByPrimaryKey(Integer id);

    List<RegionContinent> selectValid();

    List<RegionContinent> selectAll();

    List<RegionContinent> selectList(@Param("continentName") String continentName , @Param("status")Integer status);

    int updateByPrimaryKeySelective(RegionContinent record);

    int updateByPrimaryKey(RegionContinent record);
}