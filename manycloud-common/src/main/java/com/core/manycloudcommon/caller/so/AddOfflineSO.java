package com.core.manycloudcommon.caller.so;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 添加线下财务账单SO
 */
@Data
public class AddOfflineSO {

    /** 金额 **/
    private BigDecimal amountNum;

    /** 交易方式(0:银行卡,1:微信,2:支付宝) **/
    private Integer way;

    /** 0:收入,1:支出 **/
    private Integer direction;

    /** 标签(0:渠道返佣,1:退款,2:采购支出) **/
    private Integer tag;

    /** 关联信息 **/
    private String association;

    /** 发生时间 **/
    private String occurTime;

    /** 备注 **/
    private String remark;

}
