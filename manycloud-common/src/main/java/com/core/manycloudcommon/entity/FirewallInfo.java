
package com.core.manycloudcommon.entity;

import lombok.Data;

import java.util.Date;

/**
 * 安全组信息
 **/
@Data
public class FirewallInfo {
    private Integer id;
    private String firewallId;
    private String name;
    private String instanceId;
    private String userId;
    private String platformLabel;
    private String port;
    private String protocol;
    private String description;
    private Integer status;
    private Date createTime;
    private Date updateTime;

}
