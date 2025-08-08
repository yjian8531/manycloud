package com.core.manycloudadmin.so.region;

import lombok.Data;

/**
 * 更新特性协议SO
 */
@Data
public class UpdateAttributeSO {

    private Integer id;

    /** 名称 **/
    private String name;

    /** 类型(0:特性,1:协议) **/
    private Integer type;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

    /** 备注 **/
    private String remark;

}
