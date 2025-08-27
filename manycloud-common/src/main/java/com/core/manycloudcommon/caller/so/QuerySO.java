package com.core.manycloudcommon.caller.so;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
/**
 * 查询
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuerySO {

    /** 实例ID集合 **/
    private List<String> instanceIds;

    /** 共享带宽ID(RU需要) **/
    private String shareId;

    /** 每页数量 **/
    private Integer pageSize;
    /** 页码 **/
    private Integer pageNum;
    /** 订单号 **/
    private String orderNo;

    /** VPS编码 **/
    private String vpsCode;

}
