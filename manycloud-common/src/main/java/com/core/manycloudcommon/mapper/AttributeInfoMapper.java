package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.AttributeInfo;
import com.core.manycloudcommon.vo.main.ContrastNodeVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AttributeInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(AttributeInfo record);

    int insertSelective(AttributeInfo record);

    AttributeInfo selectByPrimaryKey(Integer id);

    List<AttributeInfo> selectByNodeId(@Param("nodeId") Integer nodeId,@Param("type")Integer type);

    List<AttributeInfo> selectValidByType(@Param("type")Integer type);

    List<AttributeInfo> selectByType(@Param("type")Integer type);

    List<ContrastNodeVO> selectContrastNode(@Param("cityId") Integer cityId);

    List<AttributeInfo> selectList(@Param("type")Integer type,@Param("name")String name);

    int updateByPrimaryKeySelective(AttributeInfo record);

    int updateByPrimaryKey(AttributeInfo record);
}