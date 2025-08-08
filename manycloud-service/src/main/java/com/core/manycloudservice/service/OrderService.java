package com.core.manycloudservice.service;

import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.so.order.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {


    /**
     * 查询节点的周期类型
     * @param queryPeriodTypeSelectSO
     * @return
     */
    ResultMessage queryPeriodTypeSelect(QueryPeriodTypeSelectSO queryPeriodTypeSelectSO);

    /**
     * 查询购买价格
     * @param orderSO
     * @return
     */
    BigDecimal queryOrderPrice(OrderSO orderSO);


    /**
     * 添加订单
     * @param userId
     * @param orderSO
     * @return
     */
    ResultMessage add(String userId, OrderSO orderSO);



    /***
     * 查询购物车列表
     * @param queryShoppingListSO
     * @return
     */
    ResultMessage queryShoppingList(String userId, QueryShoppingListSO queryShoppingListSO);


    /**
     * 查询购物车订单详细
     * @param queryShoppingDetailSO
     * @return
     */
    ResultMessage queryShoppingDetail(QueryShoppingDetailSO queryShoppingDetailSO);


    /**
     * 删除购物车信息
     * @param delShoppingSO
     * @return
     */
    ResultMessage delShopping(DelShoppingSO delShoppingSO);



    /**
     * 结算订单(购买)
     * @param orderNos
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    ResultMessage buy(String userId, List<String> orderNos,BigDecimal amount);


    /***
     * 查询续费价格
     * @param renewSO
     * @return
     */
    BigDecimal queryRenewPrice(RenewSO renewSO);



    /***
     * 续费
     * @param renewSO
     * @return
     */
    ResultMessage renew(RenewSO renewSO);

}
