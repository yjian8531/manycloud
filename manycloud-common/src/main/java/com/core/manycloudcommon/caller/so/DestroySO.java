package com.core.manycloudcommon.caller.so;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 销毁
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DestroySO {

    /** 实例ID **/
    private String instanceId;

}
