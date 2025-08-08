package com.core.manycloudadmin.so.region;

import lombok.Data;

/**
 * 添加区域大洲SO
 */
@Data
public class AddContinentSO {

    /** 大洲名称 **/
    private String continentName;

    /** 排序优先级(级别越低优先越高) **/
    private Integer sorting;

    /** 备注 **/
    private String remark;

}
