package com.core.manycloudservice.so.user;

import lombok.Data;

/**
 * 查询系统消息列表SO
 */
@Data
public class QuerySysLogListSO {

    /** 状态(0:未读,1:已读) **/
    private Integer status;

    private Integer page;
    private Integer pageSize;

}
