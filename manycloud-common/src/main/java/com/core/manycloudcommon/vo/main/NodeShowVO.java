package com.core.manycloudcommon.vo.main;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class NodeShowVO {

    /** 节点区域ID **/
    private Integer nodeId;
    /** 节点区域名称 **/
    private String nodeName;
    /** 特性集合 **/
    private List<String> attributeList;
    /** 最低价格 **/
    private BigDecimal minPrice;
}
