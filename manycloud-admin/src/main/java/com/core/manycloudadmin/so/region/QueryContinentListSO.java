package com.core.manycloudadmin.so.region;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

/**
 * 查询区域大洲列表SO
 */
@Data
public class QueryContinentListSO extends BaseQueryPageSO {

    /** 大洲名称 **/
    private String continentName;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

}
