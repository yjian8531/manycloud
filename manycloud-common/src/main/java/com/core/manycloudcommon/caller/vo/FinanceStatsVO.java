package com.core.manycloudcommon.caller.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class FinanceStatsVO {
    /** 时间 **/
    private List<String> dates;
    /** 金额 **/
    private List<BigDecimal> recharge;
    /** 充值金额 **/
    private List<BigDecimal> consumption;
    /** 提现金额 **/
    private List<BigDecimal> withdrawal;
    /** 总金额 **/
    private Map<String, Integer> total;

}
