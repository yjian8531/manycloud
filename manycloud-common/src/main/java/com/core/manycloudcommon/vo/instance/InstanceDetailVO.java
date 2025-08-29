package com.core.manycloudcommon.vo.instance;

import lombok.Builder;
import lombok.Data;

/**
 * 实例详情数据VO
 */
@Data
@Builder
public class InstanceDetailVO {

    /** 实例ID **/
    private String instanceId;

    /** 节点名称 **/
    private String nodeName;

    /** 公网IP **/
    private String publicIp;

    /** 私网IP **/
    private String privateIp;

    /** 端口 **/
    private Integer connectPort;

    /** CPU **/
    private String cpu;

    /** 内存 **/
    private String ram;

    /** 账号 **/
    private String account;

    /** 密码 **/
    private String pwd;

    /** 系统盘 **/
    private String sysDisk;

    /** 数据盘 **/
    private String dataDisk;

    /** 带宽 **/
    private String bandwidth;

    /** 带宽单位 **/
    private String bandwidthUnit;

    /** 流量 **/
    private String flow;

    /** 流量单位 **/
    private String flowUnit;

    /** 镜像 **/
    private String image;

    /** 状态 **/
    private Integer status;

    /** 创建时间 **/
    private String createTime;

    /** 结束时间 **/
    private String endTime;


}
