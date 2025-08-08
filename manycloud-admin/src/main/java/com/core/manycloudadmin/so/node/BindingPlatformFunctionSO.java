package com.core.manycloudadmin.so.node;

import lombok.Data;

import java.util.List;

/**
 * 绑定资源平台功能SO
 */
@Data
public class BindingPlatformFunctionSO {

    /** 资源平台ID **/
    private Integer id;

    /** 功能ID集合 **/
    private List<Integer> functionIds;

}
