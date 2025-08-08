package com.core.manycloudadmin.so.node;

import lombok.Data;

/**
 * 更新可用区节点信息SO
 */
@Data
public class UpdateNodeInfoSO {

    private Integer id;
    /** 平台标签 **/
    private String label;

    /** 节点值 **/
    private String nodeVal;

    /** 节点参数 **/
    private String nodeParam;

    /** 所属城市 **/
    private Integer cityId;

    /** 可用区节点名称 **/
    private String nodeName;

    /** 排序优先级(级别越低优先越高) **/
    private Integer sorting;

    /** 备注 **/
    private String remark;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

}
