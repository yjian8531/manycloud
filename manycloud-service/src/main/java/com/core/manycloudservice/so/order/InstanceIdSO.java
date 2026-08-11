package com.core.manycloudservice.so.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主机实例ID请求SO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceIdSO {

    /** 主机实例ID **/
    private String instanceId;

}
