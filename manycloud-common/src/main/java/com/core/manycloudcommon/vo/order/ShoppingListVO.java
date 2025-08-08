package com.core.manycloudcommon.vo.order;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车VO
 */
@Data
public class ShoppingListVO {

    /** ID **/
    private Integer id;

    /** 订单编号 **/
    private String orderNo;

    /** 名称 **/
    private String name;

    /** 配置 **/
    private String config;

    /** 数量 **/
    private Integer num;

    /** 价格 **/
    private BigDecimal price;

    /** 状态(0:待支付,1:确认中) **/
    private Integer status;

    /** 周期 **/
    private String period;

    /** 购买时长 **/
    private Integer duration;

}
