package com.core.manycloudservice.controller;

import com.core.manycloudcommon.caller.so.CreateSecuritySO;
import com.core.manycloudcommon.caller.so.QueryFirewallSO;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.filter.SpecialKeyInterceptor;
import com.core.manycloudservice.service.OpenInstanceService;
import com.core.manycloudservice.so.order.ApiOrderSO;
import com.core.manycloudservice.so.order.InstanceIdSO;
import com.core.manycloudservice.so.order.MultiRegionOrderSO;
import com.core.manycloudservice.so.order.RenewSO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 特殊用户开放接口（/api/instance）
 * 鉴权：请求头 X-Private-Key（SpecialKeyInterceptor 校验后把 userId 放到 request 属性）
 * 所有接口的 userId 均来自私钥，不走登录态
 */
@Slf4j
@RestController
@RequestMapping("/api/instance")
public class ApiInstanceController {

    @Autowired
    private OpenInstanceService openInstanceService;

    /** 下单（直接购买，无购物车；走余额扣款；参数简化为：nodeId、modelId、imageId、period、quantity） */
    @PostMapping("/create")
    public ResultMessage create(@RequestBody ApiOrderSO apiOrderSO, HttpServletRequest request) {
        return openInstanceService.create(getSpecialUserId(request), apiOrderSO);
    }

    /** 多地区下单 （支持不同地区的多台主机同时下单） */
    @PostMapping("/createMultiRegion")
    public ResultMessage createMultiRegion(@RequestBody MultiRegionOrderSO orderSO, HttpServletRequest request) {
        return openInstanceService.createMultiRegion(getSpecialUserId(request), orderSO);
    }


    /** 主机详情 */
    @PostMapping("/detail")
    public ResultMessage detail(@RequestBody InstanceIdSO so, HttpServletRequest request) {
        return openInstanceService.detail(getSpecialUserId(request), so.getInstanceId());
    }

    /** 主机创建状态（好了没） */
    @PostMapping("/status")
    public ResultMessage status(@RequestBody InstanceIdSO so, HttpServletRequest request) {
        return openInstanceService.status(getSpecialUserId(request), so.getInstanceId());
    }

    /** 销毁 */
    @PostMapping("/destroy")
    public ResultMessage destroy(@RequestBody InstanceIdSO so, HttpServletRequest request) {
        return openInstanceService.destroy(getSpecialUserId(request), so.getInstanceId());
    }

    /** 续费 */
    @PostMapping("/renew")
    public ResultMessage renew(@RequestBody RenewSO renewSO, HttpServletRequest request) {
        return openInstanceService.renew(getSpecialUserId(request), renewSO);
    }

    /** 创建/放行安全组端口 */
    @PostMapping("/firewall/create")
    public ResultMessage createFirewall(@RequestBody CreateSecuritySO createSecuritySO, HttpServletRequest request) {
        return openInstanceService.createFirewall(getSpecialUserId(request), createSecuritySO);
    }

    /** 查询安全组规则 */
    @PostMapping("/firewall/query")
    public ResultMessage queryFirewall(@RequestBody QueryFirewallSO queryFirewallSO, HttpServletRequest request) {
        return openInstanceService.queryFirewall(getSpecialUserId(request), queryFirewallSO);
    }


    /** 从 request 属性取私钥鉴权后的特殊用户 userId */
    private String getSpecialUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(SpecialKeyInterceptor.ATTR_SPECIAL_USER_ID);
        return userId == null ? null : userId.toString();
    }
}
