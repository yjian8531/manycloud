package com.core.manycloudadmin.so.admin;

import lombok.Data;

import java.util.List;

/***
 * 绑定特性协议SO
 */
@Data
public class BindingAttributeSO {

    /** 特性协议ID **/
    private Integer id;

    /** 平台标签 **/
    private String label;

    /** 节点ID集合 **/
    private List<Integer> nodeIds;

}
