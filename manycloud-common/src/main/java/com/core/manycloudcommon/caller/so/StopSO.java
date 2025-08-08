package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

/**
 * 关机
 */
@Data
@Builder
public class StopSO {

    /** 实例ID **/
    private String instanceId;
}
