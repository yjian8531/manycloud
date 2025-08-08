package com.core.manycloudservice.so.main;

import lombok.Data;

/**
 * 查询节点售卖详细信息SO
 */
@Data
public class QueryBuyNodeDetailSO {

    /** 配置ID **/
    private Integer modelId;

    /** 节点ID **/
    private Integer nodeId;
    /** CPU **/
    private String cpu;
    /** 内存 **/
    private String ram;

}
