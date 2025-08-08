package com.core.manycloudcommon.vo.main;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 产品列表数据VO
 */
@Data
public class QueryProductDetailVO {

    /** 节点ID **/
    private Integer nodeId;

    /** 国家名称 **/
    private String countryName;

    /** 省份名称 **/
    private String provinceName;

    /** 城市Id **/
    private Integer cityId;

    /** 城市名称 **/
    private String cityName;

    /** 节点名称 **/
    private String nodeName;

    /** CPU **/
    private String cpu;

    /** 内存 **/
    private String ram;

    /** 配置ID **/
    private Integer modelId;

    /** 配置价格 **/
    private BigDecimal modelPrice;

    /** 固定配置标记(Y/N) **/
    private String regular;

    /** 磁盘大小 **/
    private Integer diskNum;
    /** 磁盘基数 **/
    private Integer diskItem;
    /** 磁盘价格 **/
    private BigDecimal diskPrice;

    /** 带宽限制(Y/N) **/
    private String bandwidthExtend;
    /** 带宽大小 **/
    private Integer bandwidthNum;
    /** 带宽基数 **/
    private Integer bandwidthItem;
    /** 带宽价格 **/
    private BigDecimal bandwidthPrice;
    /** 带宽单位 **/
    private String bandwidthType;

    /** 流量限制(Y/N) **/
    private String flowExtend;
    /** 流量大小 **/
    private Integer flowNum;
    /** 流量基数 **/
    private Integer flowItem;
    /** 流量价格 **/
    private BigDecimal flowPrice;
    /** 流量单位 **/
    private String flowType;

    /** 总价格 **/
    private BigDecimal totalPrice;

}
