package com.core.manycloudcommon.caller.Item;

import lombok.Data;

@Data
public class FinanceStatsItem {
    private String dateStr;
    private Integer recharge;
    private Integer consumption;
    private Integer withdrawal;

}
