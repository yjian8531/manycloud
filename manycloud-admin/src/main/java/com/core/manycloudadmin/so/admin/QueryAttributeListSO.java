package com.core.manycloudadmin.so.admin;

import lombok.Data;

/**
 * 查询特性列表SO
 */
@Data
public class QueryAttributeListSO {

    /** 名称 **/
    private String name;

    /** 类型(0:特性,1:协议) **/
    private Integer type;

    private Integer page;

    private Integer pageSize;


}
