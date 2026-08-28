package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

/**
 * 防火墙删除SO
 */
@Data
@Builder
public class DeleteFirewallSO {

    /** 防火墙ID **/
    private String fwId;
}
