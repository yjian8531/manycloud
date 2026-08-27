package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * 创建防火墙
 */
public class GrantFirewallSO {
    private String groupId;
    private String instanceId;

    //阿里云需要参数
    private String firewallTemplateId; // 防火墙模板 ID

}
