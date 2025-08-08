package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.RegionCity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RegionCityMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(RegionCity record);

    int insertSelective(RegionCity record);

    RegionCity selectByPrimaryKey(Integer id);

    RegionCity selectByName(@Param("name")String name);

    List<RegionCity> selectAll();

    List<RegionCity> selectByProvince(@Param("provinceId") Integer provinceId);

    List<RegionCity> selectBySuperior(@Param("id")Integer id,@Param("type")Integer type);

    List<RegionCity> selectList(@Param("superiorId")Integer superiorId,@Param("superiorLevel")Integer superiorLevel,@Param("cityName")String cityName,@Param("status")Integer status);

    int updateByPrimaryKeySelective(RegionCity record);

    int updateByPrimaryKey(RegionCity record);
}