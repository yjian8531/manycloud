package com.core.manycloudadmin.service;

import com.core.manycloudadmin.so.user.QueryUserListSO;
import com.core.manycloudadmin.so.user.UpdateUserFinanceSO;
import com.core.manycloudadmin.so.user.UpdateUserRemarkSO;
import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.utils.ResultMessage;

public interface UserService {

    /**
     * 查询用户列表
     *
     * @param queryUserListSO
     * @return
     */
    ResultMessage queryList(QueryUserListSO queryUserListSO);


    /**
     * 更新用户备注信息
     *
     * @param updateUserRemarkSO
     * @return
     */
    ResultMessage updateUserRemark(UpdateUserRemarkSO updateUserRemarkSO);


    /**
     * 管理员更新用户余额
     *
     * @param updateUserFinanceSO
     * @return
     */
    ResultMessage updateUserFinance(UpdateUserFinanceSO updateUserFinanceSO);


    /**
     * 获取用户统计
     *
     * @param
     * @return
     */
    ResultMessage queryTotalUser(UserStatsSo userStatsSo);


    /**
     * 获取用户推广统计
     *
     * @param queryCommissionStatisticsSO
     * @return
     */
    ResultMessage queryCommissionStatistics(QueryCommissionStatisticsSO queryCommissionStatisticsSO);



    /**
     * 获取用户推广明细列表
     *
     * @param
     * @param
     * @param queryUserProListSO
     * @return
     */
    ResultMessage queryUserProList(QueryUserProListSO queryUserProListSO);


    /**
     * 查询返佣明细列表(客户端)
     *
     * @param queryCommissionDetailListSO
     * @return
     */
    ResultMessage queryCommissionDetailList(QueryCommissionDetailListSO queryCommissionDetailListSO);



    /**
     * 查询用户等级列表
     ***/
    ResultMessage queryUserLevelList(QueryUserLevelListSO queryUserLevelListSO);

}

