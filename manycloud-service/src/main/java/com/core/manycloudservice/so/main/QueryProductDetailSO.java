package com.core.manycloudservice.so.main;

import lombok.Data;

import java.util.List;

/**
 * 查询产品列表数据SO
 */
@Data
public class QueryProductDetailSO {

    /** 地域ID **/
    private Integer regionId;

    /** 地域类型(1:大洲,2:国家,3:省份,4:城市) **/
    private Integer regionType;

    /** 特性ID集合 **/
    private List<Integer> attributeIds;

    /** 排序(0:正常，1：CPU升序，2：CPU降序，3：内存升序，4：内存降序) **/
    private Integer sort;

    private Integer page;

    private Integer pageSize;

}
