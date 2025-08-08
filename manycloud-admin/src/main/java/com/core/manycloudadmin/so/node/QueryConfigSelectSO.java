package com.core.manycloudadmin.so.node;

import lombok.Data;

/**
 * 查询节点配置选择SO
 */
@Data
public class QueryConfigSelectSO {

    /** 节点ID **/
    private Integer nodeId;

    /** 类型(model:基础配置,disk:磁盘,network:网络) **/
    private String configType;

}
