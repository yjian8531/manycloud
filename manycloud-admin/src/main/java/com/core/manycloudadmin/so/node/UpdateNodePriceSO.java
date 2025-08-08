package com.core.manycloudadmin.so.node;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新节点价格SO
 */
@Data
public class UpdateNodePriceSO {

    private Integer id;

    /** 节点ID **/
    private Integer nodeId;

    /** 类型(model:基础配置,disk:磁盘,network:网络) **/
    private String configType;

    /** 配置ID **/
    private Integer configId;

    /** 周期(0:天,1:月) **/
    private Integer period;

    /** 价格基数 **/
    private BigDecimal item;

    /** 单价 **/
    private BigDecimal price;

    /** 备注 **/
    private String remark;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

}
