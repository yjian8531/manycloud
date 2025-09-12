package com.core.manycloudcommon.caller.so;

import lombok.Data;

@Data
public class QueryUserProListSO {
    /** 手机或邮箱（模糊查询） */
    private String account;
    /** 开始时间 */
    private String startTime;
    /** 结束时间 */
    private String endTime;
    private Integer pageNum;
    private Integer pageSize;

    private String userId;
    private String adminId;
}