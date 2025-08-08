package com.core.manycloudadmin.so.node;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

@Data
public class QueryNodePriceListSO extends BaseQueryPageSO {

    /** 节点ID **/
    private Integer nodeId;

    /** 类型(model:基础配置,disk:磁盘,network:网络) **/
    private String configType;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

}
