package com.core.manycloudservice.so.instance;

import lombok.Data;

import java.util.List;

/**
 * 添加产品分组管理
 */
@Data
public class DelGroupProductS0 {
    /** 产品ID **/
    private List<String> instanceIds;
}
