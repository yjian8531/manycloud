package com.core.manycloudcommon.caller.so;

import lombok.Data;

/**
 * 用户统计查询参数
 */
@Data
public class UserStatsSo {
    /** 统计粒度：day(天)/month(月)/year(年) */
    private String timeUnit;
    /** 开始日期：yyyy-MM-dd（按年查时传 yyyy-01-01 即可） */
    private String startTime;
    /** 结束日期：yyyy-MM-dd（按年查时传 yyyy-12-31 即可） */
    private String endTime;
    /** 是否包含失活用户：默认true  */
    private Boolean includeInactive = true;
}
