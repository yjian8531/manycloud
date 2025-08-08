package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.NodeModel;
import com.core.manycloudcommon.vo.main.QueryProductDetailVO;
import com.core.manycloudcommon.vo.node.NodeModelConfigVO;
import org.apache.ibatis.annotations.Param;

import javax.xml.soap.Node;
import java.util.List;

public interface NodeModelMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(NodeModel record);

    int insertSelective(NodeModel record);

    NodeModel selectByPrimaryKey(Integer id);

    NodeModel selectByConfig(@Param("nodeId") Integer nodeId,@Param("cpu") String cpu,@Param("ram") String ram);

    List<NodeModel> selectByNode(@Param("nodeId") Integer nodeId);

    List<NodeModel> selectList(@Param("nodeId") Integer nodeId,@Param("cpu") String cpu,@Param("ram") String ram,@Param("status") Integer status);

    int updateByPrimaryKeySelective(NodeModel record);

    int updateByPrimaryKey(NodeModel record);

    List<QueryProductDetailVO> selectProductDetail(@Param("list") List<Integer> list, @Param("sort")Integer sort);

    List<NodeModelConfigVO> selectConfigByNodeId(@Param("nodeId") Integer nodeId);
}