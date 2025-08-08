package com.core.manycloudservice.controller;

import com.core.manycloudcommon.controller.BaseController;
import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.service.FinanceService;
import com.core.manycloudservice.so.finance.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/finance")
public class FinanceController extends BaseController {

    @Autowired
    private FinanceService financeService;


    /**
     * 查询用户账单明细列表数据
     * @return
     */
    @PostMapping("/query/list/user")
    public ResultMessage queryListUser(@RequestBody QueryListSO queryListSO){
        UserInfo userInfo = this.getLoginUser();
        queryListSO.setUserId(userInfo.getUserId());
        return financeService.queryList(queryListSO);
    }


    /**
     * 推广奖励
     * @param promotionCountSO
     */
    @PostMapping("/promotion/count")
    public void promotionCount(@RequestBody PromotionCountSO promotionCountSO){
        financeService.promotionCount(promotionCountSO.getUserId(),promotionCountSO.getProductNo(),promotionCountSO.getAmount(),promotionCountSO.getType());
    }



    /***
     * 查询推广统计(客户端)
     * @return
     */
    @GetMapping("/query/commission/statistics")
    public ResultMessage queryCommissionStatistics(){
        UserInfo userInfo = this.getLoginUser();
        return financeService.queryCommissionStatistics(userInfo.getUserId());
    }


    /**
     * 查询推广明细列表(客户端)
     * @param queryUserProListSO
     * @return
     */
    @PostMapping("/query/userpro/list")
    public ResultMessage queryUserProList(@RequestBody QueryUserProListSO queryUserProListSO){
        UserInfo userInfo = this.getLoginUser();
        queryUserProListSO.setUserId(userInfo.getUserId());
        return financeService.queryUserProList(queryUserProListSO);
    }


    /**
     * 查询返佣明细列表(客户端)
     * @param queryCommissionDetailListSO
     * @return
     */
    @PostMapping("/query/commissiondetail/list")
    public ResultMessage queryCommissionDetailList(@RequestBody QueryCommissionDetailListSO queryCommissionDetailListSO){
        UserInfo userInfo = this.getLoginUser();
        queryCommissionDetailListSO.setUserId(userInfo.getUserId());
        return financeService.queryCommissionDetailList(queryCommissionDetailListSO);
    }


    /***
     * 查询提现余额和税点
     * @return
     */
    @GetMapping("/get/withdrawal/info")
    public ResultMessage getWithdrawalInfo(){
        UserInfo userInfo = this.getLoginUser();
        return financeService.getWithdrawalInfo(userInfo.getUserId());
    }


    /***
     * 添加提现申请
     * @param addWithdrawalSO
     * @return
     */
    @PostMapping("/add/withdrawal")
    public ResultMessage addWithdrawal(@RequestBody AddWithdrawalSO addWithdrawalSO){
        UserInfo userInfo = this.getLoginUser();
        addWithdrawalSO.setUserId(userInfo.getUserId());
        return financeService.addWithdrawal(addWithdrawalSO);
    }


    /**
     * 查询用户提现列表信息
     * @param queryWithdrawalSO
     * @return
     */
    @PostMapping("/query/withdrawal/list")
    public ResultMessage queryWithdrawal(@RequestBody QueryWithdrawalSO queryWithdrawalSO){
        UserInfo userInfo = this.getLoginUser();
        queryWithdrawalSO.setUserId(userInfo.getUserId());
        return financeService.queryWithdrawal(queryWithdrawalSO);
    }


}
