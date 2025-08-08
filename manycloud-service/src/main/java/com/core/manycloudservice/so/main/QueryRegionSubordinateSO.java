package com.core.manycloudservice.so.main;

import lombok.Data;

/**
 * 查询地域下级信息
 */
@Data
public class QueryRegionSubordinateSO {

    /** 地域ID **/
    private Integer regionId;

    /** 地域类型(1:大洲,2:国家,3:省份,4:城市) **/
    private Integer regionType;

}
