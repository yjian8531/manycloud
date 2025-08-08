package com.core.manycloudcommon.vo.admin;

import lombok.Builder;
import lombok.Data;

/**
 * 平台功能绑定VO
 */
@Data
@Builder
public class PlatformBdFunctionVO {

    /** 绑定ID **/
    private Integer id;

    /** 平台ID **/
    private Integer platformId;

    /** 功能ID **/
    private Integer functionId;

    /** 功能名称 **/
    private String functionName;

    /** 绑定标记(Y/N) **/
    private String tad;

}
