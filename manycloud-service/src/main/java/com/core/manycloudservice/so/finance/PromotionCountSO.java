package com.core.manycloudservice.so.finance;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 推广奖励SO
 */
@Data
public class PromotionCountSO {

    /** 消费用户ID **/
    private String userId;
    /** 产品ID **/
    private String productNo;
    /** 消费金额 **/
    private BigDecimal amount;
    /** 类型(0:购买,1:续费) **/
    private Integer type;

}
