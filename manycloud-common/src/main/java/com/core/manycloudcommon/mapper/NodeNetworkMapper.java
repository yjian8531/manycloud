package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.NodeNetwork;
import com.core.manycloudcommon.vo.node.NodeModelConfigVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NodeNetworkMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(NodeNetwork record);

    int insertSelective(NodeNetwork record);

    NodeNetwork selectByPrimaryKey(Integer id);

    NodeNetwork selectByNode(@Param("nodeId") Integer nodeId,@Param("type")Integer type, @Param("modelId")Integer modelId);

    List<NodeNetwork> selectByNodeIds(@Param("list") List<Integer> list);

    List<NodeNetwork> selectList(@Param("nodeId") Integer nodeId);

    List<NodeModelConfigVO> selectConfigByNodeId(@Param("nodeId") Integer nodeId);

    int updateByPrimaryKeySelective(NodeNetwork record);

    int updateByPrimaryKey(NodeNetwork record);
}