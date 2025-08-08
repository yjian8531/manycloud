package com.core.manycloudservice.so.main;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 查询购买价格SO
 */
@Data
@Builder
public class QueryBuyPriceSO {

    /** 可用区ID **/
    private Integer nodeId;

    /** CPU **/
    private String cpu;

    /** 内存 **/
    private String ram;

    /** 系统盘大小 **/
    private BigDecimal sysDisk;

    /** 数据盘大小 **/
    private BigDecimal dataDisk;

    /** 带宽 **/
    private BigDecimal bandwidth;

    /** 流量 **/
    private BigDecimal flow;

    /** 镜像类型 **/
    private String imageType;

    /** 镜像参数 **/
    private String imageParam;

    /** 购买数量 **/
    private Integer num;

    /** 周期 **/
    private Integer period;

    /** 购买时长 **/
    private Integer time;

}
