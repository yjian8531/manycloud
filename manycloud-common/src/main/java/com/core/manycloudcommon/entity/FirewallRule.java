package com.core.manycloudcommon.entity;

import lombok.Data;

import java.util.Date;
@Data
public class FirewallRule {
    private Integer id;
    private String firewallId;
    private String protocol;
    private String port;
    private String ipAddress;
    private String action;
    private String priority;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
