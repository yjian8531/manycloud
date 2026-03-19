package com.core.manycloudservice.so.main;

import lombok.Data;

/**
 * 查询节点可用区的配置信息SO
 */
@Data
public class QueryBuyNodeModelSO {
    /** 节点可用区ID **/
    private Integer nodeId;

    /** 系统类型 (可选，如 windows、ubuntu、centos 等) **/
    private String imageType;
}
