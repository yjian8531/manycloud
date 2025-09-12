package com.core.manycloudcommon.caller.so;

import lombok.Data;

@Data
public class QueryCommissionStatisticsSO {
    /** 推广用户账号 */
    private String account;
    private Integer pageNum;
    private Integer pageSize;
}