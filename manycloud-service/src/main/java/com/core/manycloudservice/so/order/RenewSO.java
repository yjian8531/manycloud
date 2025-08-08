package com.core.manycloudservice.so.order;

import lombok.Data;

/**
 * 续费SO
 */
@Data
public class RenewSO {

    private String instanceId;

    private Integer duration;

}
