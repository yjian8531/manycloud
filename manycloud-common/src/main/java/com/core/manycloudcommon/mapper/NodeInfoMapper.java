package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.NodeInfo;
import com.core.manycloudcommon.vo.main.BaseProductVO;
import com.core.manycloudcommon.vo.node.QueryNodeListVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface NodeInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(NodeInfo record);

    int insertSelective(NodeInfo record);

    NodeInfo selectByPrimaryKey(Integer id);

    List<NodeInfo> selectByLabel(@Param("label") String label);

    List<NodeInfo> selectAll();

    String selectNick(@Param("nodeId") Integer nodeId);

    int updateByPrimaryKeySelective(NodeInfo record);

    int updateByPrimaryKey(NodeInfo record);

    List<BaseProductVO> selectBaseProduct(@Param("continentId") Integer continentId, @Param("countryId")Integer countryId,
                                          @Param("screen")String screen, @Param("list")List<Integer> list);

    /**
     * 查询地域最低价格
     * @param type （地域类型1:大洲,2:国家，3:省份,4:城市，5：区域节点）
     * @param regionId 地域ID
     * @return
     */
    BigDecimal queryNodeMinPrice(@Param("type") Integer type, @Param("regionId")Integer regionId,@Param("list")List<Integer> list);

    /**
     * 查询地域 产品总数
     * @param type （地域类型3:省份,4:城市）
     * @param regionId 地域ID
     * @return
     */
    int queryNodeProductNum(@Param("type") Integer type, @Param("regionId")Integer regionId,@Param("list")List<Integer> list);


    List<NodeInfo> selectByRegionId(@Param("regionType") Integer regionType, @Param("regionId")Integer regionId);

    List<NodeInfo> selectBdAttByRegionId(@Param("regionType") Integer regionType, @Param("regionId")Integer regionId,@Param("list")List<Integer> list);

    List<QueryNodeListVO> selectList(@Param("continentId") Integer continentId, @Param("countryId")Integer countryId,@Param("provinceId") Integer provinceId,
                                     @Param("cityId")Integer cityId, @Param("name")String name, @Param("status")Integer status);
}