package com.core.manycloudcommon.vo.finance;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 查询客户推广统计
 */
@Data
public class QueryCommissionStatisticsVO {

    /** 推广用户总数 **/
    private Integer userNum;

    /** 产品总数 **/
    private Integer productNum;

    /** 消费总额 **/
    private BigDecimal consumptionTotal;

    /** 返佣总额 **/
    private BigDecimal commissionTotal;

    /** 渠道等级名称 **/
    private String name;

    /** 推广码 **/
    private String market;

    /** 推广链接 **/
    private String marketUrl;


}
