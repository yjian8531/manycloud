package com.core.manycloudcommon.vo.instance;

import lombok.Data;

import java.util.Date;

/**
 * 用户实例展示VO
 */
@Data
public class InstanceUserVO {

    /** 实例ID **/
    private String instanceId;
    /** 别名 **/
    private String nike;
    /** 配置 **/
    private String config;
    /** 公网IP **/
    private String publicIp;
    /** 私网IP **/
    private String privateIp;
    /** 电源状态：halted(停止)、running(运行中),execution(执行中) **/
    private String powerState;
    /** 状态(0:待创建,1:创建中,2:预留,3:使用中,4:待续费,5:已过期,6:已销毁) **/
    private Integer status;
    /** 周期(0:天,1:月) **/
    private Integer period;
    /** 创建时间 **/
    private Date createTime;
    /** 到期时间 **/
    private Date endTime;
    /** 可用区名称 **/
    private String nodeName;
    /** 分组名称 **/
    private String groupName;
    /** 所属账号 **/
    private String account;

}
