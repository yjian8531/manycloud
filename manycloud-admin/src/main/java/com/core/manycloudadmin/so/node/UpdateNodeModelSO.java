package com.core.manycloudadmin.so.node;

import lombok.Data;

/**
 * 添更新节点机型配置SO
 */
@Data
public class UpdateNodeModelSO {

    private Integer id;

    /** 节点ID **/
    private Integer nodeId;

    /** CPU **/
    private String cpuVal;

    /** 内存 **/
    private String ramVal;

    /** 规格参数 **/
    private String modelParam;

    /** 固定配置标记(Y/N) **/
    private String regular;

    /** 备注 **/
    private String remark;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

}
