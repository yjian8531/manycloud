package com.core.manycloudadmin.so.region;

import lombok.Data;

/***
 * 更新大洲信息SO
 */
@Data
public class UpdateContinentSO {

    private Integer id;

    /** 大洲名称 **/
    private String continentName;

    /** 排序优先级(级别越低优先越高) **/
    private Integer sorting;

    /** 备注 **/
    private String remark;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

}
