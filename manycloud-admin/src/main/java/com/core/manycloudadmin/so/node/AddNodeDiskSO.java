package com.core.manycloudadmin.so.node;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 添加节点磁盘配置SO
 */
@Data
public class AddNodeDiskSO {

    /** 节点ID **/
    private Integer nodeId;

    /** 规格配置ID **/
    private Integer modelId;

    /** 磁盘类型 **/
    private String diskType;

    /** 磁盘参数 **/
    private String modelParam;

    /** 磁盘最小限制 **/
    private BigDecimal minNum;

    /** 磁盘最大限制 **/
    private BigDecimal maxNum;

    /** 基础大小 **/
    private BigDecimal itemNum;

    /** 默认赠送大小 **/
    private BigDecimal giveNum;

    /** 是否支持扩展标记(Y/N) **/
    private String extendBl;

    /** 是否支持数据盘标记(Y/N) **/
    private String dataBl;

    /** 备注 **/
    private String remark;

}
