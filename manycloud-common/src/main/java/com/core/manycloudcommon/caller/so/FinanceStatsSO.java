package com.core.manycloudcommon.caller.so;

import lombok.Data;

@Data
public class FinanceStatsSO {
    /** 时间单位 **/
    private String timeUnit; // day, month, year
    /** 开始时间 **/
    private String startTime;
    /** 结束时间 **/
    private String endTime;
}
