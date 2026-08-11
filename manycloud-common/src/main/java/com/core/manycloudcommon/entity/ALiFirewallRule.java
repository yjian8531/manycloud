package com.core.manycloudcommon.entity;

import lombok.Data;

@Data
public class ALiFirewallRule {
    private String ruleProtocol; // TCP/UDP/TCP+UDP/ICMP
    private String port;         // 端口范围，如 "8080" 或 "1024/1055"
    private String sourceCidrIp; // 源 IP CIDR，如 "0.0.0.0/0"
    private String remark;       // 备注
}
