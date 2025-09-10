package com.core.manycloudadmin.service;

import com.core.manycloudadmin.so.finance.ExecWithdrawalSO;
import com.core.manycloudadmin.so.finance.QueryListSO;
import com.core.manycloudadmin.so.finance.QueryWithdrawalListSO;
import com.core.manycloudadmin.so.finance.QueyrSaleDetailSO;
import com.core.manycloudcommon.caller.so.FinanceStatsSO;
import com.core.manycloudcommon.utils.ResultMessage;
import org.springframework.transaction.annotation.Transactional;

public interface FinanceService {

    /**
     * 查询账单明细列表数据
     * @return
     */
    ResultMessage queryList(QueryListSO queryListSO);


    /**
     * 获取市场推广用户
     * @return
     */
    ResultMessage querySaleUser();


    /**
     * 查询市场人员提成数据
     * @param queyrSaleDetailSO
     * @return
     */
    ResultMessage queyrSaleDetail(QueyrSaleDetailSO queyrSaleDetailSO);


    /**
     * 查询提现列表信息
     * @param queryWithdrawalListSO
     * @return
     */
    ResultMessage queryWithdrawalList(QueryWithdrawalListSO queryWithdrawalListSO);



    /**
     * 提现审核
     * @param execWithdrawalSO
     * @return
     */
    ResultMessage execWithdrawal(ExecWithdrawalSO execWithdrawalSO);

    /**
     * 获取财务统计数据
     * @param so
     * @return
     */
    ResultMessage getFinanceStats(FinanceStatsSO so);

}
