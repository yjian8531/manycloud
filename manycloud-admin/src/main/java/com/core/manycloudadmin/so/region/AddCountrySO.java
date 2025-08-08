package com.core.manycloudadmin.so.region;

import lombok.Data;

/**
 * 添加区域国家SO
 */
@Data
public class AddCountrySO {
    /** 上级区域ID **/
    private Integer superiorId;
    /** 国家名称 **/
    private String countryName;
    /** 排序优先级(级别越低优先越高) **/
    private Integer sorting;
    /** 备注 **/
    private String remark;

}
