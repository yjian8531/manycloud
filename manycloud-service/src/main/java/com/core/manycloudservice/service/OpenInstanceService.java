package com.core.manycloudservice.service;

import com.core.manycloudcommon.caller.so.CreateSecuritySO;
import com.core.manycloudcommon.caller.so.QueryFirewallSO;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.so.order.ApiOrderSO;
import com.core.manycloudservice.so.order.MultiRegionOrderSO;
import com.core.manycloudservice.so.order.RenewSO;

/**
 * 特殊用户开放接口(/api/instance)业务层
 * userId 均来自私钥鉴权(SpecialKeyInterceptor)，不走登录
 */
public interface OpenInstanceService {

    /** 下单（只传必要参数，其他配置自动查询） */
    ResultMessage create(String userId, ApiOrderSO apiOrderSO);

    /** 多地区下单（支持不同地区同时下单）  */
    ResultMessage createMultiRegion(String userId, MultiRegionOrderSO orderSO);

    /** 主机详情 */
    ResultMessage detail(String userId, String instanceId);

    /** 主机创建状态（好了没） */
    ResultMessage status(String userId, String instanceId);

    /** 销毁 */
    ResultMessage destroy(String userId, String instanceId);

    /** 续费 */
    ResultMessage renew(String userId, RenewSO renewSO);

    /** 创建/放行安全组端口 */
    ResultMessage createFirewall(String userId, CreateSecuritySO createSecuritySO);

    /** 查询安全组规则 */
    ResultMessage queryFirewall(String userId, QueryFirewallSO queryFirewallSO);
}
