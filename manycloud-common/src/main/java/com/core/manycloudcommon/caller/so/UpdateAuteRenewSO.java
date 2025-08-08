package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

/**
 * 修改自动续费
 */
@Data
@Builder
public class UpdateAuteRenewSO {

    /** 实例ID **/
    private String instanceId;
    /** 标识(0:打开,1:关闭) **/
    private Integer tad;

}
