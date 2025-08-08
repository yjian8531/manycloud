package com.core.manycloudcommon.vo.main;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 全球云服务器列表数据
 */
@Data
public class BaseProductVO {

    /** 大洲 **/
    private String continent;
    /** 大洲ID **/
    private Integer continentId;
    /** 国家 **/
    private String country;
    /** 国家ID **/
    private Integer countryId;
    /** 省份 **/
    private String province;
    /** 省份ID **/
    private Integer provinceId;
    /** 城市 **/
    private String city;
    /** 城市ID **/
    private Integer cityId;
    /** 区域节点数量 **/
    private Integer nodeNum;
    /** 产品数量 **/
    private Integer productNum;
    /** 产品最低价格 **/
    private BigDecimal minPrice;



}
