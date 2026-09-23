package com.core.manycloudservice.service;

import com.core.manycloudcommon.entity.InstanceInfo;

/**
 * 订单异常表(t_order_exce)写入服务
 */
public interface OrderExceService {

    /***
     * 创建/续费失败写入订单异常表 t_order_exce
     * @param instanceInfo
     * @param content 失败原因
     */
    void save(InstanceInfo instanceInfo, String content);
}
