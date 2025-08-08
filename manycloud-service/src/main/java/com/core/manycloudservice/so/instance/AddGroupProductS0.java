package com.core.manycloudservice.so.instance;

import lombok.Data;

import java.util.List;

/**
 * 添加产品分组管理
 */
@Data
public class AddGroupProductS0 {

    /** 分组ID **/
    private Integer groupId;

    /** 实例ID集合 **/
    private List<String> instanceIds;
}
