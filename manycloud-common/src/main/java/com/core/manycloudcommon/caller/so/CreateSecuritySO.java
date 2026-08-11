package com.core.manycloudcommon.caller.so;

import com.core.manycloudcommon.entity.ALiFirewallRule;
import lombok.Data;

import java.util.List;

@Data
public class CreateSecuritySO {
     /**
     * 实例ID（关联的主机实例ID）
     */
    private String instanceId;

    /**
     * 安全组名称
     */
    private String name;

    /**
     * 开放端口
     */
    private String port;

    /**
     * 安全组id
     */
    private String fwId;

    /**
     * 协议
     */
    private String protocol;

    //阿里云需要字段
    private String description;

    //阿里云需要
    /**
     * 防火墙规则列表
     */
    private List<ALiFirewallRule> firewallRules;

    /**
     * 新接口格式：防火墙名称（兼容新接口）
     */
    private String firewallName;

    /**
     * 新接口格式：防火墙规则列表
     */
    private List<FirewallRule> rules;

    /**
     * 新接口格式的防火墙规则
     */
    @Data
    public static class FirewallRule {
        /**
         * 协议类型：TCP/UDP/ICMP/ALL
         */
        private String protocol;

        /**
         * 端口号：单个端口(80)、端口范围(8080-8088)、多个端口逗号分隔(80,443,8080)
         */
        private String port;

        /**
         * 源IP地址：0.0.0.0/0 表示允许所有IP
         */
        private String source;

        /**
         * 规则描述
         */
        private String description;
    }
}
