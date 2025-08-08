package com.core.manycloudcommon.vo.node;

import lombok.Data;

import java.util.Date;

/**
 * 可用区节点列表VO
 */
@Data
public class QueryNodeListVO {

    /** 可用区ID **/
    private Integer id;

    /** 可用区名称 **/
    private String name;

    /** 平台标签 **/
    private String label;

    /** 平台名称 **/
    private String platformName;

    /** 节点值 **/
    private String nodeVal;

    /** 可用区参数 **/
    private String nodeParam;

    /** 排序优先级(级别越低优先越高) **/
    private Integer sorting;

    /** 备注 **/
    private String remark;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

    /** 最后更新时间 **/
    private Date updateTime;

    /** 主机数量 **/
    private Integer instanceNum;

}
