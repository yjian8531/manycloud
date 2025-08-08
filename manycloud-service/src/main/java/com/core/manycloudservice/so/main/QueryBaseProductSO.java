package com.core.manycloudservice.so.main;

import lombok.Data;

import java.util.List;

/**
 * 全球云服务器收索
 */
@Data
public class QueryBaseProductSO {

    /** 大洲ID **/
    private Integer continentId;

    /** 国家ID **/
    private Integer countryId;

    /** 特性ID集合 **/
    private List<Integer> attributeIds;

    /** 查询内容(省份-城市) **/
    private String screen;

    private Integer page;

    private Integer pageSize;

}
