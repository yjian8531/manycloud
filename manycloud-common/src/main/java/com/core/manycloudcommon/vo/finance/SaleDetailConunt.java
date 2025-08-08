package com.core.manycloudcommon.vo.finance;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 市场提成统计VO
 */
@Data
public class SaleDetailConunt {

    /** 总消费金额 **/
    private BigDecimal consumption;

    /** 总提成金额 **/
    private BigDecimal reward;

    /** 消费次数 **/
    private Integer num;
}
