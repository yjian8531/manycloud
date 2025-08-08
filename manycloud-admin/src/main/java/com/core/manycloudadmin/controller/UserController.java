package com.core.manycloudadmin.controller;

import com.core.manycloudadmin.service.UserService;
import com.core.manycloudadmin.so.user.QueryUserListSO;
import com.core.manycloudadmin.so.user.UpdateUserFinanceSO;
import com.core.manycloudadmin.so.user.UpdateUserRemarkSO;
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
}
