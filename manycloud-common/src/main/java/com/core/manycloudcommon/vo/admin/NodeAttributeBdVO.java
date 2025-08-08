package com.core.manycloudcommon.vo.admin;

import lombok.Data;

/**
 * 节点特性绑定VO
 */
@Data
public class NodeAttributeBdVO {

    private Integer id;

    /** 节点ID **/
    private Integer nodeId;

    /** 特性ID **/
    private Integer attributeId;

    /** 节点名称 **/
    private String nodeName;

    /** 绑定标记(Y/N) **/
    private String tad;

}
