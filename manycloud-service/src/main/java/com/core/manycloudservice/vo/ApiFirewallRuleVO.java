package com.core.manycloudservice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API开放接口 - 防火墙规则响应VO（友好格式）
 * 专为 /api/ 接口设计，字段简洁清晰，无null值干扰
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiFirewallRuleVO {

    /**
     * 协议类型：TCP/UDP/ICMP/ALL
     */
    private String protocol;

    /**
     * 端口号：单个端口(80)、端口范围(8080-8088)、ALL(所有端口)
     */
    private String port;

    /**
     * 源IP地址：0.0.0.0/0 表示允许所有IP
     */
    private String source;

    /**
     * 动作：ACCEPT(允许)/DROP(拒绝)
     */
    private String action;

    /**
     * 规则描述
     */
    private String description;
}
