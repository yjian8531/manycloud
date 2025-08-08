package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.NodeDisk;
import com.core.manycloudcommon.entity.NodeModel;
import com.core.manycloudcommon.vo.node.NodeModelConfigVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NodeDiskMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(NodeDisk record);

    int insertSelective(NodeDisk record);

    NodeDisk selectByPrimaryKey(Integer id);

    NodeDisk selectByNode(@Param("nodeId") Integer nodeId, @Param("modelId")Integer modelId);

    List<NodeDisk> selectByNodeIds(List<Integer> list);

    List<NodeModelConfigVO> selectConfigByNodeId(@Param("nodeId") Integer nodeId);

    List<NodeDisk> selectList(@Param("nodeId") Integer nodeId);

    int updateByPrimaryKeySelective(NodeDisk record);

    int updateByPrimaryKey(NodeDisk record);
}