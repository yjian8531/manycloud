package com.core.manycloudservice.so.order;

import lombok.Data;

/**
 * 查询购物车信息
 */
@Data
public class QueryShoppingListSO {

    private Integer page;

    private Integer pageSize;

}
