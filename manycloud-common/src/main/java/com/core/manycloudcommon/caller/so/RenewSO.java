package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

/**
 * 续费
 */
@Data
@Builder
public class RenewSO {

    /** 实例ID **/
    private String instanceId;
    /** 周期类型(0:月,1:天) **/
    private Integer type;
    /** 周期数量 **/
    private int num;

}
