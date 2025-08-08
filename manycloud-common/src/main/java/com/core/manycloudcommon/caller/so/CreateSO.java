package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

/**
 * 创建
 */
@Data
@Builder
public class CreateSO {

    /** 主机密码 **/
    private String pwd;
    /** 规格ID **/
    private String bundleId;
    /** 镜像ID **/
    private String imageId;
    /** 时长 **/
    private Integer period;
    /** 数量 **/
    private Integer num;

    /** 磁盘类型 **/
    private String disksType;
    /** 磁盘大小 **/
    private Integer disksSize;
    /** 分区 **/
    private String zone;
    /** cpu **/
    private String cpu;
    /** 内存 **/
    private String ram;
    /** 云主机类型(RU 和 Ucloud 平台需要) **/
    private String machineType;
    /** 防火墙ID **/
    private String securityGroupId;

}
