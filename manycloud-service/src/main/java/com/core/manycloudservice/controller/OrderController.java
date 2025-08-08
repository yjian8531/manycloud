package com.core.manycloudservice.controller;

import com.core.manycloudcommon.controller.BaseController;
import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.service.OrderService;
import com.core.manycloudservice.so.main.QueryBuyPriceSO;
import com.core.manycloudservice.so.order.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 用户Controller
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController extends BaseController {

    @Autowired
    private OrderService orderService;


    /**
     * 查询节点的周期类型
     * @param queryPeriodTypeSelectSO
     * @return
     */
    @PostMapping("/query/periodtype")
    public ResultMessage queryPeriodTypeSelect(@RequestBody QueryPeriodTypeSelectSO queryPeriodTypeSelectSO){
        return orderService.queryPeriodTypeSelect(queryPeriodTypeSelectSO);
    }

    /**
     * 查询购买价格
     * @param orderSO
     * @return
     */
    @PostMapping("/query/price")
    public ResultMessage queryOrderPrice(@RequestBody OrderSO orderSO){
        BigDecimal price = orderService.queryOrderPrice(orderSO);
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,price);
    }


    /**
     * 添加订单
     * @param orderSO
     * @return
     */
    @PostMapping("/add")
    public ResultMessage add(@RequestBody OrderSO orderSO){
        UserInfo userInfo = this.getLoginUser();
        return orderService.add(userInfo.getUserId(),orderSO);
    }

    /***
     * 查询购物车列表
     * @param queryShoppingListSO
     * @return
     */
    @PostMapping("/query/shopping")
    public ResultMessage queryShoppingList(@RequestBody QueryShoppingListSO queryShoppingListSO){
        UserInfo userInfo = this.getLoginUser();
        return orderService.queryShoppingList(userInfo.getUserId(),queryShoppingListSO);
    }


    /**
     * 查询购物车订单详细
     * @param queryShoppingDetailSO
     * @return
     */
    @PostMapping("/query/shopping/detail")
    public ResultMessage queryShoppingDetail(@RequestBody QueryShoppingDetailSO queryShoppingDetailSO){
        return orderService.queryShoppingDetail(queryShoppingDetailSO);
    }


    /**
     * 删除购物车信息
     * @param delShoppingSO
     * @return
     */
    @PostMapping("/del/shopping")
    public ResultMessage delShopping(@RequestBody DelShoppingSO delShoppingSO){
        return orderService.delShopping(delShoppingSO);
    }



    /**
     * 结算订单(购买)
     * @param buySO
     * @return
     */
    @PostMapping("/buy")
    public ResultMessage buy(@RequestBody BuySO buySO){
        UserInfo userInfo = this.getLoginUser();
        return orderService.buy(userInfo.getUserId(),buySO.getOrderNos(),buySO.getAmount());
    }


    /***
     * 查询续费价格
     * @param renewSO
     * @return
     */
    @PostMapping("/query/renew/price")
    public ResultMessage queryRenewPrice(@RequestBody RenewSO renewSO){
        BigDecimal price = orderService.queryRenewPrice(renewSO);
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,price);
    }



    /***
     * 续费
     * @param renewSO
     * @return
     */
    @PostMapping("renew")
    public ResultMessage renew(@RequestBody RenewSO renewSO){
        return orderService.renew(renewSO);
    }

}
