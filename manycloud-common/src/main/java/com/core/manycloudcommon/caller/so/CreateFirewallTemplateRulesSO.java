package com.core.manycloudcommon.caller.so;

import com.core.manycloudcommon.entity.ALiFirewallRule;
import lombok.Data;

import java.util.List;

@Data
public class CreateFirewallTemplateRulesSO {
    private String regionId; // 地域 ID（由 caller 内部设置）
    private String firewallTemplateId; // 防火墙模板 ID
    private List<ALiFirewallRule> firewallRules; // 规则列表;
}
