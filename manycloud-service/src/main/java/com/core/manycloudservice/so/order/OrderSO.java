package com.core.manycloudservice.so.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 查询购买价格SO
 */
@Data
@Builder
public class OrderSO {

    /** 可用区ID **/
    private Integer nodeId;

    /** 基础配置ID **/
    private Integer modelId;

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

    /** 镜像版本 **/
    private String image;

    /** 镜像参数 **/
    private Integer imageId;

    /** 购买数量 **/
    private Integer num;

    /** 周期 **/
    private Integer period;

    /** 购买时长 **/
    private Integer duration;

    /** 用户ID **/
    private String userId;

}
