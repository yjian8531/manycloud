package com.core.manycloudcommon.caller.aliyun;

import lombok.Data;

import java.util.List;

@Data
    public  class FirewallTemplate {
        private String firewallTemplateId;
        private String name;
        private String description;
        private String creationTime;
        private String createTime;
        private List<FirewallTemplateRule> firewallTemplateRules;
    }