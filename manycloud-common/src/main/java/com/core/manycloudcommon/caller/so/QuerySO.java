package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

import java.util.List;
/**
 * 查询
 */
@Data
@Builder
public class QuerySO {

    /** 实例ID集合 **/
    private List<String> instanceIds;

    /** 共享带宽ID(RU需要) **/
    private String shareId;

}
