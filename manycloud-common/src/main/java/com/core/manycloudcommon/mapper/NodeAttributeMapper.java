package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.NodeAttribute;
import com.core.manycloudcommon.vo.admin.NodeAttributeBdVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NodeAttributeMapper {
    int deleteByPrimaryKey(Integer id);

    int deleteByAttributeId(@Param("attributeId") Integer attributeId);

    int deleteByIds(@Param("list") List<Integer> list);

    int insert(NodeAttribute record);

    int insertSelective(NodeAttribute record);

    int insertList(@Param("list") List<NodeAttribute> list);

    NodeAttribute selectByPrimaryKey(Integer id);

    List<NodeAttributeBdVO> selectNodeByAttributeId(@Param("attributeId") Integer attributeId,@Param("label") String label);

    List<Integer> selectByAttributeIds(@Param("list")List<Integer> list,@Param("size") Integer size);

    int updateByPrimaryKeySelective(NodeAttribute record);

    int updateByPrimaryKey(NodeAttribute record);
}