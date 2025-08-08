package com.core.manycloudadmin.controller;


import com.core.manycloudadmin.service.InstanceService;
import com.core.manycloudadmin.so.instance.ExecPowerSO;
import com.core.manycloudadmin.so.instance.QueryDetailSO;
import com.core.manycloudadmin.so.instance.QueryListSO;
import com.core.manycloudadmin.so.instance.RenewSO;
import com.core.manycloudcommon.utils.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/instance")
public class InstanceController {

    @Autowired
    private InstanceService instanceService;

    /***
     * 查询实例列表
     * @param queryListSO
     * @return
     */
    @PostMapping(value = "/query/list",produces = {"application/json"})
    public ResultMessage queryList(@RequestBody QueryListSO queryListSO){
        return instanceService.queryList(queryListSO);
    }


    /**
     * 查询实例详情信息
     * @param queryDetailSO
     * @return
     */
    @PostMapping(value = "/query/detail",produces = {"application/json"})
    public ResultMessage queryDetail(@RequestBody QueryDetailSO queryDetailSO){
        return instanceService.queryDetail(queryDetailSO);
    }


    /**
     * 主机电源操作
     * @param
     * @return
     */
    @PostMapping(value = "/exec/power",produces = {"application/json"})
    public ResultMessage execPower(@RequestBody ExecPowerSO execPowerSO){
        return instanceService.execPower(execPowerSO);
    }


    /***
     * 查询续费价格
     * @param renewSO
     * @return
     */
    @PostMapping("/query/renew/price")
    public ResultMessage queryRenewPrice(@RequestBody RenewSO renewSO){
        BigDecimal price = instanceService.queryRenewPrice(renewSO);
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,price);
    }



    /***
     * 续费
     * @param renewSO
     * @return
     */
    @PostMapping("renew")
    public ResultMessage renew(@RequestBody RenewSO renewSO){
        return instanceService.renew(renewSO);
    }

}
