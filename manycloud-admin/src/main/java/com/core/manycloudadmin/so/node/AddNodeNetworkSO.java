package com.core.manycloudadmin.so.node;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 添加节点网络配置SO
 */
@Data
public class AddNodeNetworkSO {

    /** 节点ID **/
    private Integer nodeId;

    /** 规格ID **/
    private Integer modelId;

    /** 类型(0:带宽,1:流量) **/
    private Integer type;

    /** 网络类型 **/
    private String networkType;

    /** 网络参数 **/
    private String networkParam;

    /** 最低限制 **/
    private BigDecimal minNum;

    /** 最高限制 **/
    private BigDecimal maxNum;

    /** 基础大小 **/
    private BigDecimal itemNum;

    /** 限制标记(Y:限制/N:不限) **/
    private String extendBl;

    /** 备注 **/
    private String remark;

}
