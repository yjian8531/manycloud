package com.core.manycloudcommon.vo.admin;

import lombok.Builder;
import lombok.Data;

/**
 * 地域下拉选择VO
 */
@Data
@Builder
public class QueryRegionSelectVO {

    /** 上级地域ID **/
    private Integer superiorId;

    /** 地域名称 **/
    private String name;

}
