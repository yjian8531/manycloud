package com.core.manycloudadmin.so.admin;

import lombok.Data;

/**
 * 添加特性协议SO
 */
@Data
public class AddAttributeSO {

    /** 名称 **/
    private String name;

    /** 类型(0:特性,1:协议) **/
    private Integer type;

    /** 备注 **/
    private String remark;

}
