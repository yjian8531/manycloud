package com.core.manycloudadmin.so.region;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

/**
 * 分页查询城市列表so
 */
@Data
public class QueryCityListSO extends BaseQueryPageSO {

    /** 上级区域级别(1:大洲,2:国家,3:省份) **/
    private Integer superiorLevel;
    /** 上级区域ID **/
    private Integer superiorId;
    /** 城市名称 **/
    private String cityName;
    /** 状态(0:正常,1:禁用) **/
    private Integer status;


}
