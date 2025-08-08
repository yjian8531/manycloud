package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

/**
 * 重启
 */
@Data
@Builder
public class RebootSO {

    /** 实例ID **/
    private String instanceId;

}
