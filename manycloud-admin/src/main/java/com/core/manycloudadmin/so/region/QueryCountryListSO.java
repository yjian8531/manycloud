package com.core.manycloudadmin.so.region;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

/**
 * 查询区域国家列表SO
 */
@Data
public class QueryCountryListSO extends BaseQueryPageSO {

    /** 上级区域级别(1:大洲,2:国家) **/
    private Integer superiorLevel;
    /** 上级区域ID **/
    private Integer superiorId;
    /** 国家名称 **/
    private String countryName;
    /** 状态(0:正常,1:禁用) **/
    private Integer status;

}
