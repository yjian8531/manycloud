package com.core.manycloudservice.so.main;

import lombok.Data;

/**
 * 查询地域导航信息
 */
@Data
public class QueryRegionNavigationSO {

    /** 地域ID **/
    private Integer regionId;

    /** 地域类型(1:大洲,2:国家,3:省份,4:城市) **/
    private Integer regionType;

}
