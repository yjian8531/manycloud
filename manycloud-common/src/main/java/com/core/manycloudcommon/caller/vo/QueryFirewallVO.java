package com.core.manycloudcommon.caller.vo;

import com.core.manycloudcommon.entity.FirewallRule;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QueryFirewallVO {
    private String code;      // 操作状态码
    private String msg;       // 操作描述
    private String groupId;   // 防火墙组ID
    private String fwId;      // 防火墙ID
    private String name;      // 防火墙名称
    private List<FirewallRule> rules; // 新增规则信息列表字段

}