package com.core.manycloudadmin.so.node;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

@Data
public class QueryNodeModelListSO extends BaseQueryPageSO {
    /** 节点ID **/
    private Integer nodeId;

    /** CPU **/
    private String cpu;

    /** 内存 **/
    private String ram;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

}
