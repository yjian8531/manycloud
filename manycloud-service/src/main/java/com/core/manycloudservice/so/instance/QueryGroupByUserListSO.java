package com.core.manycloudservice.so.instance;

import lombok.Data;

/**
 * 分页查询用户产品分组信息
 */
@Data
public class QueryGroupByUserListSO {

    /** 组名 **/
    private String name;

    private Integer page;

    private Integer pageSize;

}
