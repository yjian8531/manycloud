package com.core.manycloudadmin.controller;

import com.core.manycloudadmin.service.UserService;
import com.core.manycloudadmin.so.user.QueryUserListSO;
import com.core.manycloudadmin.so.user.UpdateUserFinanceSO;
import com.core.manycloudadmin.so.user.UpdateUserRemarkSO;
import com.core.manycloudcommon.caller.so.QueryCommissionDetailListSO;
import com.core.manycloudcommon.caller.so.QueryCommissionStatisticsSO;
import com.core.manycloudcommon.caller.so.QueryUserLevelListSO;
import com.core.manycloudcommon.caller.so.QueryUserProListSO;
import com.core.manycloudcommon.entity.AdminInfo;
import com.core.manycloudcommon.utils.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 查询用户列表
     * @param queryUserListSO
     * @return
     */
    @PostMapping(value = "/query/list",produces = {"application/json"})
    public ResultMessage queryList(@RequestBody QueryUserListSO queryUserListSO){
        return userService.queryList(queryUserListSO);
    }



    /**
     * 更新用户备注信息
     * @param updateUserRemarkSO
     * @return
     */
    @PostMapping(value = "/update/remark",produces = {"application/json"})
    public ResultMessage updateUserRemark(@RequestBody UpdateUserRemarkSO updateUserRemarkSO){
        return userService.updateUserRemark(updateUserRemarkSO);
    }


    /**
     * 管理员更新用户余额
     * @param updateUserFinanceSO
     * @return
     */
    @PostMapping(value = "/update/finance",produces = {"application/json"})
    public ResultMessage updateUserFinance(@RequestBody UpdateUserFinanceSO updateUserFinanceSO){
        return userService.updateUserFinance(updateUserFinanceSO);
    }


    /**
     * 查询用户推广统计信息
     * @param
     * @return
     */
    @PostMapping("/query/commission/statistics/list")
    public ResultMessage queryCommissionStatistics(@RequestBody QueryCommissionStatisticsSO queryCommissionStatisticsSO) {
        return userService.queryCommissionStatistics(queryCommissionStatisticsSO);
    }

    /**
     * 查询用户推广明细列表
     * @param
     * @return
     */
    @PostMapping("/query/userpro/list/admin")
    public ResultMessage queryUserProList(@RequestBody QueryUserProListSO queryUserProListSO) {
        return userService.queryUserProList(queryUserProListSO);
    }

    /**
     * 查询返佣明细列表(管理后台)
     * @param queryCommissionDetailListSO
     * @return
     */
    @PostMapping(value = "/query/commission/list/admin",produces = {"application/json"})
    public ResultMessage queryCommissionDetailListAdmin(@RequestBody QueryCommissionDetailListSO queryCommissionDetailListSO){
        return userService.queryCommissionDetailList(queryCommissionDetailListSO);
    }

    /**
     * 用户等级列表（管理后台）
     * @param queryUserLevelListSO
     * @return
     */

    @PostMapping(value = "/query/userLevel/list",produces = {"application/json"})
    public ResultMessage queryUserLevelToList(@RequestBody QueryUserLevelListSO queryUserLevelListSO){
        return userService.queryUserLevelList(queryUserLevelListSO);
    }
}
