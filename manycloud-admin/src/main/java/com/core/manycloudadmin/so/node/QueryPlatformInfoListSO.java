package com.core.manycloudadmin.so.node;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

/**
 * 查询资源平台列表SO
 */
@Data
public class QueryPlatformInfoListSO extends BaseQueryPageSO {

    /** 平台标签 **/
    private String label;

    /** 平台名称 **/
    private String name;

}
