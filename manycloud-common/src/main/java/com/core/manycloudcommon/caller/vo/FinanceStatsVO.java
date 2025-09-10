package com.core.manycloudcommon.caller.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FinanceStatsVO {
    private List<String> dates;
    private List<Integer> recharge;
    private List<Integer> consumption;
    private List<Integer> withdrawal;
    private Map<String, Integer> total;

}
