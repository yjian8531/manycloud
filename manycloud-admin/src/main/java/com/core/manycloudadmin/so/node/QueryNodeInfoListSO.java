package com.core.manycloudadmin.so.node;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

/**
 * 分页查询可用区列表SO
 */
@Data
public class QueryNodeInfoListSO extends BaseQueryPageSO {

    /** 可用区名称 **/
    private String name;

    /** 大洲ID **/
    private Integer continentId;

    /** 国家ID **/
    private Integer countryId;

    /** 省份ID **/
    private Integer provinceId;

    /** 城市ID **/
    private Integer cityId;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

}
