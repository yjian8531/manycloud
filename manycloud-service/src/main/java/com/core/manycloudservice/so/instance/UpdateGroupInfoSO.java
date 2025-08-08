package com.core.manycloudservice.so.instance;

import lombok.Data;

/**
 * 更新产品分组信息
 */
@Data
public class UpdateGroupInfoSO {

    private Integer id;

    /** 名称 **/
    private String name;
    /**  备注 **/
    private String remark;

}
