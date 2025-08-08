package com.core.manycloudservice.so.user;

import lombok.Data;

/**
 * 查询用户操作日志SO
 */
@Data
public class QueryUserLogListSO {

    /** 操作名称 **/
    private String alias;
    private Integer page;
    private Integer pageSize;
}
