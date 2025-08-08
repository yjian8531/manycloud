package com.core.manycloudcommon.vo.main;

import lombok.Builder;
import lombok.Data;

/***
 * 节点测试参数对比VO
 */
@Builder
@Data
public class ContrastNodeVO {

    /** 功能特性ID **/
    private Integer id;
    /** 功能特性名称 **/
    private String name;
    /** 功能特性参数 **/
    private String paramStr;
    /** 节点ID **/
    private Integer nodeId;
    /** 节点名称 **/
    private String nodeName;

}
