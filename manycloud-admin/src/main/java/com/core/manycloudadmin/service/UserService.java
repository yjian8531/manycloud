package com.core.manycloudadmin.service;

import com.core.manycloudadmin.so.user.QueryUserListSO;
import com.core.manycloudadmin.so.user.UpdateUserFinanceSO;
import com.core.manycloudadmin.so.user.UpdateUserRemarkSO;
import com.core.manycloudcommon.caller.so.UserStatsSo;
import com.core.manycloudcommon.utils.ResultMessage;

public interface UserService {

    /**
     * 查询用户列表
     * @param queryUserListSO
     * @return
     */
    ResultMessage queryList(QueryUserListSO queryUserListSO);


    /**
     * 更新用户备注信息
     * @param updateUserRemarkSO
     * @return
     */
    ResultMessage updateUserRemark(UpdateUserRemarkSO updateUserRemarkSO);


    /**
     * 管理员更新用户余额
     * @param updateUserFinanceSO
     * @return
     */
    ResultMessage updateUserFinance(UpdateUserFinanceSO updateUserFinanceSO);



    /**
     * 获取用户统计
     * @param
     * @return
     */
    ResultMessage queryTotalUser(String timeUnit, String startTime, String endTime,Boolean includeInactive);

}
