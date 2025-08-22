package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GrantFirewallSO {
    private String groupId;
    private String instanceId;

}
