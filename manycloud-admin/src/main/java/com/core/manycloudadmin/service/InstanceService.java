package com.core.manycloudadmin.service;

import com.core.manycloudadmin.so.instance.ExecPowerSO;
import com.core.manycloudadmin.so.instance.QueryDetailSO;
import com.core.manycloudadmin.so.instance.QueryListSO;
import com.core.manycloudadmin.so.instance.RenewSO;
import com.core.manycloudcommon.caller.so.PlatformSo;
import com.core.manycloudcommon.utils.ResultMessage;

import java.math.BigDecimal;

public interface InstanceService {

    /***
     * 查询实例列表
     * @param queryListSO
     * @return
     */
    ResultMessage queryList(QueryListSO queryListSO);


    /**
     * 查询实例详情信息
     * @param queryDetailSO
     * @return
     */
    ResultMessage queryDetail(QueryDetailSO queryDetailSO);


    /**
     * 主机电源操作
     * @param
     * @return
     */
    ResultMessage execPower(ExecPowerSO execPowerSO);


    /***
     * 查询续费价格
     * @param renewSO
     * @return
     */
    BigDecimal queryRenewPrice(RenewSO renewSO);



    /***
     * 续费
     * @param renewSO
     * @return
     */
    ResultMessage renew(RenewSO renewSO);



    /**
     * 获取平台总览数据
     * @return 平台总览数据
     */
    ResultMessage getPlatformOverview(PlatformSo  platformSo);


    /**
     * 获取配置分布
     * @param platformSo
     * @return
     */
    ResultMessage getConfigDistribution(PlatformSo  platformSo);


    /**
     * 下拉框查询所有平台
     * @param
     * @return
     */
    ResultMessage selectPlatformList();






}
