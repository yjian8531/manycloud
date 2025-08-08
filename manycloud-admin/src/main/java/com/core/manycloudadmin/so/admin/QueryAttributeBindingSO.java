package com.core.manycloudadmin.so.admin;

import lombok.Data;

/**
 * 查询特性协议绑定信息
 */
@Data
public class QueryAttributeBindingSO {

    /** 特性协议ID **/
    private Integer id;

    /** 平台标签 **/
    private String label;

}
