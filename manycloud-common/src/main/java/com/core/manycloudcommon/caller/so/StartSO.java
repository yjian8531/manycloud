package com.core.manycloudcommon.caller.so;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 开机
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSO {

    /** 实例ID **/
    private String instanceId;

}
