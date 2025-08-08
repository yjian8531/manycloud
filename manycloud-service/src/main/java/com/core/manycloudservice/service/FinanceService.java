package com.core.manycloudservice.service;

import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.so.finance.*;

import java.math.BigDecimal;

public interface FinanceService {

    /**
     * 查询账单明细列表数据
     * @return
     */
    ResultMessage queryList(QueryListSO queryListSO);


    /**
     * 推广奖励
     * @param userId 消费用户ID
     * @param amount 消费金额
     * @param type 类型(0:购买,1:续费)
     */
    void promotionCount(String userId, String productNo, BigDecimal amount, Integer type);


    /***
     * 查询推广统计(客户端)
     * @param userId
     * @return
     */
    ResultMessage queryCommissionStatistics(String userId);


    /**
     * 查询推广明细列表(客户端)
     * @param queryUserProListSO
     * @return
     */
    ResultMessage queryUserProList(QueryUserProListSO queryUserProListSO);


    /**
     * 查询返佣明细列表(客户端)
     * @param queryCommissionDetailListSO
     * @return
     */
    ResultMessage queryCommissionDetailList(QueryCommissionDetailListSO queryCommissionDetailListSO);


    /***
     * 查询提现余额和税点
     * @param userId
     * @return
     */
    ResultMessage getWithdrawalInfo(String userId);


    /***
     * 添加提现申请
     * @param addWithdrawalSO
     * @return
     */
    ResultMessage addWithdrawal(AddWithdrawalSO addWithdrawalSO);


    /**
     * 查询用户提现列表信息
     * @param queryWithdrawalSO
     * @return
     */
    ResultMessage queryWithdrawal(QueryWithdrawalSO queryWithdrawalSO);

}
