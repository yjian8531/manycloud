package com.core.manycloudcommon.caller.so;

import lombok.Data;

@Data
public class FinanceStatsSO {
    private String timeUnit; // day, month, year
    private String startTime;
    private String endTime;
}
