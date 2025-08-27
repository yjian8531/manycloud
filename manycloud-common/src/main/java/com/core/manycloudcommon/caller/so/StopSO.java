package com.core.manycloudcommon.caller.so;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关机
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StopSO {

    /** 实例ID **/
    private String instanceId;
}
