package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.NodeImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface NodeImageMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(NodeImage record);

    int insertSelective(NodeImage record);

    NodeImage selectByPrimaryKey(Integer id);

    List<NodeImage> selectByNode(@Param("nodeId") Integer nodeId);

    List<NodeImage> selectList(@Param("nodeId") Integer nodeId,@Param("imageType")String imageType);

    NodeImage selectNodeParam(@Param("nodeId") Integer nodeId,@Param("imageId")String imageId);

    int updateByPrimaryKeySelective(NodeImage record);

    int updateByPrimaryKey(NodeImage record);


    List<NodeImage> selectByImageVersion(String imageVersion);

//    List<NodeImage> selectByNodeId(Integer id);
}