package com.core.manycloudservice.so.instance;

import lombok.Data;

/**
 * 修改实例别名SO
 */
@Data
public class UpdateNikeSO {

    /** 实例ID **/
    private String instanceId;

    /** 别名 **/
    private String nike;

}
