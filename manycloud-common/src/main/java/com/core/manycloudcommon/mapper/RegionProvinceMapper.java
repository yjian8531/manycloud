package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.RegionProvince;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RegionProvinceMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(RegionProvince record);

    int insertSelective(RegionProvince record);

    RegionProvince selectByPrimaryKey(Integer id);

    List<RegionProvince> selectAll();

    List<RegionProvince> selectByCountry(@Param("countryId") Integer countryId);

    List<RegionProvince> selectBySuperior(@Param("id")Integer id,@Param("type")Integer type);

    List<RegionProvince> selectList(@Param("superiorId")Integer superiorId,@Param("superiorLevel")Integer superiorLevel,@Param("provinceName")String provinceName,@Param("status")Integer status);

    int updateByPrimaryKeySelective(RegionProvince record);

    int updateByPrimaryKey(RegionProvince record);
}