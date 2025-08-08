package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.NodePrice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NodePriceMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(NodePrice record);

    int insertSelective(NodePrice record);

    NodePrice selectByPrimaryKey(Integer id);

    /**
     * 查询 节点配置价格
     * @param nodeId 节点ID
     * @param configType 配置类型(model:基础配置,disk:磁盘,network:网络)
     * @param configId 配置ID
     * @param period 周期(0:天,1:月)
     * @return
     */
    NodePrice selectConfigPrice(@Param("nodeId") Integer nodeId, @Param("configType") String configType,
                                @Param("configId") Integer configId,@Param("period")Integer period);

    List<NodePrice> selectList(@Param("nodeId") Integer nodeId,@Param("configType") String configType,@Param("status") Integer status);

    int updateByPrimaryKeySelective(NodePrice record);

    int updateByPrimaryKey(NodePrice record);
}