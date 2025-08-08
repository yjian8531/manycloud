package com.core.manycloudadmin.so.region;

import lombok.Data;

/**
 * 更新身份信息
 */
@Data
public class UpdateProvinceSO {
    private Integer id;
    /** 上级区域ID **/
    private Integer superiorId;

    /** 上级区域级别(1:大洲,2:国家) **/
    private Integer superiorLevel;

    /** 省份名称 **/
    private String provinceName;

    /** 排序优先级(级别越低优先越高) **/
    private Integer sorting;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

    /** 备注 **/
    private String remark;

}
