package com.core.manycloudadmin.controller;

import com.core.manycloudadmin.service.FinanceService;
import com.core.manycloudadmin.service.InstanceService;

import com.core.manycloudcommon.caller.so.FinanceStatsSO;

import com.core.manycloudcommon.caller.so.PlatformSo;
import com.core.manycloudcommon.caller.so.UserStatsSo;
import com.core.manycloudcommon.utils.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



@Slf4j
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private com.core.manycloudadmin.service.UserService UserService;

    /***
     * 获取平台概览
     * @return
     */
    @PostMapping("/platform/overview")
    public ResultMessage getPlatformOverview(@RequestBody PlatformSo  platformSo) {
        return instanceService.getPlatformOverview(platformSo);
    }

    /**
     * 获取财务统计
     *
     * @param so
     * @return
     */
    @PostMapping(value = "/finance/stats",produces = {"application/json"})
    public ResultMessage getFinanceStats(@RequestBody FinanceStatsSO so) {
        return financeService.getFinanceStats(so);
    }

    /**
     * 获取配置分布
     *
     * @param platformSo
     * @return
     */
    @PostMapping(value = "/config/distribution",produces = {"application/json"})
    public ResultMessage getConfigDistribution(@RequestBody PlatformSo platformSo) {
        return instanceService.getConfigDistribution(platformSo);
    }


    /**
     * 获取用户统计信息
     *
     * @param
     * @return
     */
    @PostMapping(value = "/user/stats",produces = {"application/json"})
    public ResultMessage getUserStats(@RequestBody UserStatsSo userStatsSo) {
        return UserService.queryTotalUser(userStatsSo);
    }

    /**
     * 获取所有平台名称
     */
    @PostMapping(value = "/platform/list",produces = {"application/json"})
    public ResultMessage getPlatformList() {
        return instanceService.selectPlatformList();
    }
}
