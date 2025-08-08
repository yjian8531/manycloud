package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.RegionCountry;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RegionCountryMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(RegionCountry record);

    int insertSelective(RegionCountry record);

    RegionCountry selectByPrimaryKey(Integer id);

    List<RegionCountry> selectAll();

    List<RegionCountry> selectByContinent(@Param("continentId") Integer continentId);

    List<RegionCountry> selectList(@Param("superiorId")Integer superiorId,@Param("countryName")String countryName,@Param("status")Integer status);

    int updateByPrimaryKeySelective(RegionCountry record);

    int updateByPrimaryKey(RegionCountry record);
}