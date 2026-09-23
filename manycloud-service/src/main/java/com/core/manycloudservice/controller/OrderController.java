package com.core.manycloudservice.controller;

import com.core.manycloudcommon.controller.BaseController;
import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.utils.RedisUtil;
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
import java.util.ArrayList;
import java.util.List;

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
     * 同一订单号10秒内只放行一次请求(连点/重复提交在门口直接打回,不进service),
     * 频控标记用setEx单条原子命令写入、必带10秒过期,无残留卡死风险;
     * 放行后仍由service内Redis锁+CAS兜底
     * @param buySO
     * @return
     */
    @PostMapping("/buy")
    public ResultMessage buy(@RequestBody BuySO buySO){
        UserInfo userInfo = this.getLoginUser();

        List<String> markedNos = new ArrayList<>();
        for(String orderNo : buySO.getOrderNos()){
            String freqKey = "buy:freq:" + orderNo;
            if(RedisUtil.get(freqKey) != null){
                //本次整单打回:已标记的订单立即解除,避免用户白等10秒才能重试
                for(String marked : markedNos){
                    RedisUtil.del("buy:freq:" + marked);
                }
                return new ResultMessage(ResultMessage.FAILED_CODE,"订单已提交，请勿重复提交");
            }
            RedisUtil.setEx(freqKey,"1",10);
            markedNos.add(orderNo);
        }

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
