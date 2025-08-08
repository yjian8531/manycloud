package com.core.manycloudcommon.vo.main;

import lombok.Builder;
import lombok.Data;

/**
 * 地域导航信息
 */
@Data
@Builder
public class RegionNavigationVO {

    /** 地域ID **/
    private Integer regionId;

    /** 地域类型(1:大洲,2:国家,3:省份,4:城市) **/
    private Integer regionType;

    /** 地域名称 **/
    private String name;

}
