package com.core.manycloudadmin.so.node;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

/**
 * 分页查询资源平台账号列表SO
 */
@Data
public class QueryPlatformAccountListSO extends BaseQueryPageSO {

    /** 平台标签 **/
    private String label;

    /** 账号 **/
    private String account;

}
