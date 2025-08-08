package com.core.manycloudadmin.controller;

import com.core.manycloudadmin.service.AdminService;
import com.core.manycloudadmin.so.admin.LoginSO;
import com.core.manycloudadmin.so.admin.QueryAdminLogSO;
import com.core.manycloudcommon.controller.BaseController;
import com.core.manycloudcommon.entity.AdminInfo;
import com.core.manycloudcommon.utils.RedisUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController extends BaseController {

    @Autowired
    private AdminService adminService;

    /**
     * 登录
     * @param loginSO
     * @return
     */
    @PostMapping(value = "/login",produces = {"application/json"})
    public ResultMessage login(@RequestBody LoginSO loginSO){
        return adminService.login(loginSO);
    }

    /**
     * 获取系统操作日志列表
     * @param queryAdminLogSO
     * @return
     */
    @PostMapping(value = "/query/operation/log",produces = {"application/json"})
    public ResultMessage queryAdminLog(@RequestBody QueryAdminLogSO queryAdminLogSO){
        return adminService.queryAdminLog(queryAdminLogSO);
    }

    /**
     * 退出登录
     * @return
     */
    @GetMapping(value = "/exit",produces = {"application/json"})
    public ResultMessage exit(){
        AdminInfo adminInfo = this.getLoginAdmin();
        if(adminInfo != null){
            RedisUtil.del(adminInfo.getAdminId());
        }
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
    }

}
