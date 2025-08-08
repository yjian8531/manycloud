package com.core.manycloudadmin.so.region;

import lombok.Data;

/**
 * 添加城市SO
 */
@Data
public class AddCitySO {

    /** 上级区域ID **/
    private Integer superiorId;

    /** 上级区域级别(1:大洲,2:国家,3:省份) **/
    private Integer superiorLevel;

    /** 城市名称 **/
    private String cityName;

    /** 排序优先级(级别越低优先越高) **/
    private Integer sorting;

    /** 备注 **/
    private String remark;


}
