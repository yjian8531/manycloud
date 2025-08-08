package com.core.manycloudservice.so.order;

import lombok.Data;

/**
 * 删除购物车信息SO
 */
@Data
public class DelShoppingSO {

    /** 购物车信息ID(多个ID使用","隔开) **/
    private String ids;
}
