package com.core.manycloudadmin.controller;

import com.core.manycloudadmin.service.FinanceService;
import com.core.manycloudadmin.so.finance.ExecWithdrawalSO;
import com.core.manycloudadmin.so.finance.QueryListSO;
import com.core.manycloudadmin.so.finance.QueryWithdrawalListSO;
import com.core.manycloudadmin.so.finance.QueyrSaleDetailSO;
import com.core.manycloudcommon.utils.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/finance")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    /**
     * 查询账单明细列表数据
     * @return
     */
    @PostMapping(value = "/query/list",produces = {"application/json"})
    public ResultMessage queryList(@RequestBody QueryListSO queryListSO){
        return financeService.queryList(queryListSO);
    }


    /**
     * 获取市场推广用户
     * @return
     */
    @GetMapping(value = "/get/sale/user",produces = {"application/json"})
    public ResultMessage querySaleUser(){
        return financeService.querySaleUser();
    }


    /**
     * 查询市场人员提成数据
     * @param queyrSaleDetailSO
     * @return
     */
    @PostMapping(value = "/query/sale/detail",produces = {"application/json"})
    public ResultMessage queyrSaleDetail(@RequestBody QueyrSaleDetailSO queyrSaleDetailSO){
        return financeService.queyrSaleDetail(queyrSaleDetailSO);
    }


    /**
     * 查询提现列表信息
     * @param queryWithdrawalListSO
     * @return
     */
    @PostMapping(value = "/query/withdrawal/list",produces = {"application/json"})
    public ResultMessage queryWithdrawalList(@RequestBody QueryWithdrawalListSO queryWithdrawalListSO){
        return financeService.queryWithdrawalList(queryWithdrawalListSO);
    }



    /**
     * 提现审核
     * @param execWithdrawalSO
     * @return
     */
    @PostMapping(value = "/exec/withdrawal",produces = {"application/json"})
    public ResultMessage execWithdrawal(@RequestBody ExecWithdrawalSO execWithdrawalSO){
        return financeService.execWithdrawal(execWithdrawalSO);
    }


}
