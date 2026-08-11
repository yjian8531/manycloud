package com.core.manycloudcommon.caller.vo;

import com.core.manycloudcommon.caller.aliyun.FirewallTemplateRule;
import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class CreateFirewallTemplateRulesVO {
    private String code;
    private String msg;
    private String requestId;
    private List<FirewallTemplateRule> firewallTemplateRules;
}
