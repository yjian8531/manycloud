package com.core.manycloudcommon.caller.Item;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinanceStatsItem {
    /** 时间 **/
    private String dateStr;
    /** 充值金额 */
    private BigDecimal recharge;
    /** 消费金额 */
    private BigDecimal consumption;
    /** 提现金额 */
    private BigDecimal withdrawal;

}
