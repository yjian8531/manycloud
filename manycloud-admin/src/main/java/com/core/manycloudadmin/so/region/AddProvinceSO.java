package com.core.manycloudadmin.so.region;

import lombok.Data;

/**
 * 添加省份SO
 */
@Data
public class AddProvinceSO {

    /** 上级区域ID **/
    private Integer superiorId;

    /** 上级区域级别(1:大洲,2:国家) **/
    private Integer superiorLevel;

    /** 省份名称 **/
    private String provinceName;

    /** 排序优先级(级别越低优先越高) **/
    private Integer sorting;

    /** 备注 **/
    private String remark;

}
