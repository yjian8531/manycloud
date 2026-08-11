package com.core.manycloudservice.so.order;

import lombok.Builder;
import lombok.Data;

/**
 * 开放接口下单SO（简化版）
 * 只传必要参数，其他配置根据 modelId 自动从数据库查询
 */
@Data
@Builder
public class ApiOrderSO {

    /** 可用区ID **/
    private Integer nodeId;

    /** 基础配置ID **/
    private Integer modelId;

    /** 镜像值（String类型，如 "lhbp-mh5l70kv"**/
    private String imageId;

    /** 购买周期（月） **/
    private Integer period;

    /** 购买数量 **/
    private Integer quantity;

}
