package com.core.manycloudadmin.so.admin;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

/**
 * 查询登录日志SO
 */
@Data
public class QueryAdminLogSO extends BaseQueryPageSO {

    /** 账号 **/
    private String account;
    /** 登录时间开始 **/
    private String startTime;
    /** 登录时间结束 **/
    private String endTime;

}
