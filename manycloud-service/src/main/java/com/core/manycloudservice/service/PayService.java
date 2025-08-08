package com.core.manycloudservice.service;

import com.core.manycloudcommon.utils.ResultMessage;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface PayService {

    /**
     * 支付宝网页支付
     * @param userId 用户ID
     * @param amount 充值金额
     * @return
     */
    ResultMessage aliWebPay(String userId, List<String> orderNos, Double amount);

    /**
     * 支付宝网页支付回调
     * @param request
     */
    @Transactional(rollbackFor = Exception.class)
    void aliPayWebBack(HttpServletRequest request);


    /**
     * 微信二维码支付
     * @param money 金额
     * @return url
     */
    String weChatWebPay(String userId,List<String> orderNos,String money)throws Exception;


    /**
     * 微信支付回调
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    void weChatPayNotify(HttpServletRequest request, HttpServletResponse response);

}
