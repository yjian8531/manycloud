package com.core.manycloudadmin.so.region;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

/**
 * 分页查询省份列表SO
 */
@Data
public class QueryProvinceListSO extends BaseQueryPageSO {

    /** 上级区域级别(1:大洲,2:国家) **/
    private Integer superiorLevel;
    /** 上级区域ID **/
    private Integer superiorId;
    /** 省份名称 **/
    private String provinceName;
    /** 状态(0:正常,1:禁用) **/
    private Integer status;

}
