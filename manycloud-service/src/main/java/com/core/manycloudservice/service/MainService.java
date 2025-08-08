package com.core.manycloudservice.service;

import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.so.main.*;

import java.math.BigDecimal;

public interface MainService {

    /**
     * 根据类型查询Banner图信息
     * @param queryBannerByTypeSO
     * @return
     */
    ResultMessage queryBannerByType(QueryBannerByTypeSO queryBannerByTypeSO);

    /**
     * 查询有效的大洲信息
     * @return
     */
    ResultMessage queryContinentValid();

    /**
     * 根据大洲查询国家信息
     * @return
     */
    ResultMessage queryCountryByContinent(QueryCountryByContinentSO queryCountryByContinentSO);

    /***
     * 查询特性信息
     * @return
     */
    ResultMessage queryAttribute();

    /**
     * 查询客服信息
     * @return
     */
    ResultMessage queryCustomerServiceInfo();

    /**
     * 全球云服务器产品查询
     * @param queryBaseProductSO
     * @return
     */
    ResultMessage queryBaseProduct(QueryBaseProductSO queryBaseProductSO);


    /**
     * 查询地域导航信息
     * @param queryRegionNavigationSO
     * @return
     */
    ResultMessage queryRegionNavigation(QueryRegionNavigationSO queryRegionNavigationSO);


    /**
     * 查询地域下级信息
     * @param queryRegionSubordinateSO
     * @return
     */
    ResultMessage queryRegionSubordinate(QueryRegionSubordinateSO queryRegionSubordinateSO);

    /**
     * 根据上级ID 查询节点区域信息
     * @param queryNodeShowSO
     * @return
     */
    ResultMessage queryNodeShow(QueryNodeShowSO queryNodeShowSO);


    /**
     * 查询产品列表数据
     * @param queryProductDetailSO
     * @return
     */
    ResultMessage queryProductDetail(QueryProductDetailSO queryProductDetailSO);


    /**
     * 查询节点测试信息
     * @param queryNodeTestSO
     * @return
     */
    ResultMessage queryNodeTest(QueryNodeTestSO queryNodeTestSO);


    /**
     * 根据城市ID 查询节点可用区信息
     * @param queryBuyNodeSO
     * @return
     */
    ResultMessage queryBuyNode(QueryBuyNodeSO queryBuyNodeSO);


    /**
     * 询节点可用区的配置信息
     * @param queryBuyNodeModelSO
     * @return
     */
    ResultMessage queryBuyNodeModel(QueryBuyNodeModelSO queryBuyNodeModelSO);


    /**
     * 查询节点售卖磁盘信息
     * @param queryBuyNodeDetailSO
     * @return
     */
    ResultMessage queryBuyNodeDisk(QueryBuyNodeDetailSO queryBuyNodeDetailSO);


    /**
     * 查询节点售卖网络信息
     * @param queryBuyNodeDetailSO
     * @return
     */
    ResultMessage queryBuyNodeNetwork(QueryBuyNodeDetailSO queryBuyNodeDetailSO);


    /**
     * 查询节点镜像信息
     * @param queryBuyNodeDetailSO
     * @return
     */
    ResultMessage queryNodeImage(QueryBuyNodeDetailSO queryBuyNodeDetailSO);

}
