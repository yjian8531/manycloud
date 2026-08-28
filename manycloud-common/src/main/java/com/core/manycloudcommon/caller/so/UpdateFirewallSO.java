package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 防火墙规则更新SO（UCloud的UpdateFirewall为全量覆盖，需传入旧规则+新规则）
 */
@Data
@Builder
public class UpdateFirewallSO {

    /** 防火墙ID **/
    private String fwId;

    /** 全量规则列表（协议|端口|源IP|动作|优先级|备注） **/
    private List<String> rules;
}
