package com.core.manycloudservice.so.instance;

import lombok.Data;

import java.util.List;

/**
 * 实例电源操作SO
 */
@Data
public class ExecPowerSO {

    /** 电源操作：startUp（开机） | shutDown（关机） | restart（重启） **/
    private String tag;

    /** 实例ID集合 **/
    private List<String> instanceIds;
}
