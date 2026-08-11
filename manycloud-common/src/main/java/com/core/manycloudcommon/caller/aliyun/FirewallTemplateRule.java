package com.core.manycloudcommon.caller.aliyun;

import lombok.Data;

@Data
    public  class FirewallTemplateRule {
        private String firewallTemplateRuleId;
        private String ruleProtocol;
        private String port;
        private String sourceCidrIp;
        private String remark;
    }