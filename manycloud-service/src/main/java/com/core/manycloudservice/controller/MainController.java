package com.core.manycloudservice.controller;

import com.core.manycloudcommon.controller.BaseController;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.service.MainService;
import com.core.manycloudservice.so.main.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/main")
public class MainController extends BaseController {

    @Autowired
    private MainService mainService;

    /**
     * 根据类型查询Banner图信息
     * @param queryBannerByTypeSO
     * @return
     */
    @PostMapping("/query/banner")
    public ResultMessage queryBannerByType(@RequestBody QueryBannerByTypeSO queryBannerByTypeSO){
        return mainService.queryBannerByType(queryBannerByTypeSO);
    }

    /**
     * 查询有效的大洲信息
     * @return
     */
    @GetMapping("/query/continent")
    public ResultMessage queryContinentValid(){
        return mainService.queryContinentValid();
    }

    /**
     * 根据大洲查询国家信息
     * @return
     */
    @PostMapping("/query/country")
    public ResultMessage queryCountryByContinent(@RequestBody QueryCountryByContinentSO queryCountryByContinentSO){
        return mainService.queryCountryByContinent(queryCountryByContinentSO);
    }

    /***
     * 查询特性信息
     * @return
     */
    @GetMapping("/query/attribute")
    public ResultMessage queryAttribute(){
        return mainService.queryAttribute();
    }

    /**
     * 查询客服信息
     * @return
     */
    @GetMapping("/query/customer/service")
    public ResultMessage queryCustomerServiceInfo(){
        return mainService.queryCustomerServiceInfo();
    }

    /**
     * 全球云服务器产品查询
     * @param queryBaseProductSO
     * @return
     */
    @PostMapping("/query/base/product")
    public ResultMessage queryBaseProduct(@RequestBody QueryBaseProductSO queryBaseProductSO){
        return mainService.queryBaseProduct(queryBaseProductSO);
    }

    /**
     * 查询地域导航信息
     * @param queryRegionNavigationSO
     * @return
     */
    @PostMapping("/query/region/navigation")
    public ResultMessage queryRegionNavigation(@RequestBody QueryRegionNavigationSO queryRegionNavigationSO){
        return mainService.queryRegionNavigation(queryRegionNavigationSO);
    }

    /**
     * 查询地域下级信息
     * @param queryRegionSubordinateSO
     * @return
     */
    @PostMapping("/query/region/subordinate")
    public ResultMessage queryRegionSubordinate(@RequestBody QueryRegionSubordinateSO queryRegionSubordinateSO){
        return mainService.queryRegionSubordinate(queryRegionSubordinateSO);
    }

    /**
     * 根据上级ID 查询节点区域信息
     * @param queryNodeShowSO
     * @return
     */
    @PostMapping("/query/node/show")
    public ResultMessage queryNodeShow(@RequestBody QueryNodeShowSO queryNodeShowSO){
        return mainService.queryNodeShow(queryNodeShowSO);
    }

    /**
     * 查询产品列表数据
     * @param queryProductDetailSO
     * @return
     */
    @PostMapping("/query/product/detail")
    public ResultMessage queryProductDetail(@RequestBody QueryProductDetailSO queryProductDetailSO){
        return mainService.queryProductDetail(queryProductDetailSO);
    }

    /**
     * 查询节点测试信息
     * @param queryNodeTestSO
     * @return
     */
    @PostMapping("/query/node/test")
    public ResultMessage queryNodeTest(@RequestBody QueryNodeTestSO queryNodeTestSO){
        return mainService.queryNodeTest(queryNodeTestSO);
    }


    /**
     * 根据城市ID 查询节点可用区信息
     * @param queryBuyNodeSO
     * @return
     */
    @PostMapping("/query/buy/node")
    public ResultMessage queryBuyNode(@RequestBody QueryBuyNodeSO queryBuyNodeSO){
        return mainService.queryBuyNode(queryBuyNodeSO);
    }


    /**
     * 询节点可用区的配置信息
     * @param queryBuyNodeModelSO
     * @return
     */
    @PostMapping("/query/node/model")
    public ResultMessage queryBuyNodeModel(@RequestBody QueryBuyNodeModelSO queryBuyNodeModelSO){
        return mainService.queryBuyNodeModel(queryBuyNodeModelSO);
    }


    /**
     * 查询节点售卖磁盘信息
     * @param queryBuyNodeDetailSO
     * @return
     */
    @PostMapping("/query/node/disk")
    public ResultMessage queryBuyNodeDisk(@RequestBody QueryBuyNodeDetailSO queryBuyNodeDetailSO){
        return mainService.queryBuyNodeDisk(queryBuyNodeDetailSO);
    }


    /**
     * 查询节点售卖网络信息
     * @param queryBuyNodeDetailSO
     * @return
     */
    @PostMapping("/query/node/network")
    public ResultMessage queryBuyNodeNetwork(@RequestBody QueryBuyNodeDetailSO queryBuyNodeDetailSO){
        return mainService.queryBuyNodeNetwork(queryBuyNodeDetailSO);
    }


    /**
     * 查询节点镜像信息
     * @param queryBuyNodeDetailSO
     * @return
     */
    @PostMapping("/query/node/image")
    public ResultMessage queryNodeImage(@RequestBody QueryBuyNodeDetailSO queryBuyNodeDetailSO){
        return mainService.queryNodeImage(queryBuyNodeDetailSO);
    }



}
