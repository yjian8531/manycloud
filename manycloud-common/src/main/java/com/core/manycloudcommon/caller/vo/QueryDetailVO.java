package com.core.manycloudcommon.caller.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryDetailVO {
    /** 实例ID **/
    private String serviceNo;

    /** 公网IP地址 **/
    private String publicIp;

    /** 私网IP地址 **/
    private String privateIp;
    /** 账号 **/
    private String account;
    /** 端口 **/
    private Integer port;

    /** 密码 **/
    private String pwd;

    /** 状态(0:待定，1:成功，2:失败) **/
    private int status;

    /** 电源状态：halted(停止)、running(运行中),execution(执行中) **/
    private String powerState;

    /** 描述 **/
    private String msg;

}
