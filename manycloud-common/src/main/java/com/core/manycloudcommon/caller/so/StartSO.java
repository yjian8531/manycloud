package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

/**
 * 开机
 */
@Data
@Builder
public class StartSO {

    /** 实例ID **/
    private String instanceId;

}
