package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

/**
 * 销毁
 */
@Data
@Builder
public class DestroySO {

    /** 实例ID **/
    private String instanceId;

}
