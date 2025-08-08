package com.core.manycloudservice.so.main;

import lombok.Data;

import java.util.List;

/**
 * 查询城市下级区域节点信息so
 */
@Data
public class QueryNodeShowSO {

    /** 地域ID **/
    private Integer regionId;

    /** 地域类型(1:大洲,2:国家,3:省份,4:城市) **/
    private Integer regionType;

    /** 特性ID集合 **/
    private List<Integer> attributeIds;

}
