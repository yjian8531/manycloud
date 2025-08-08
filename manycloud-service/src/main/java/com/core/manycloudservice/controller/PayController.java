package com.core.manycloudservice.controller;

import com.core.manycloudcommon.controller.BaseController;
import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.service.PayService;
import com.core.manycloudservice.so.pay.AliPaySO;
import com.core.manycloudservice.so.pay.WeChatPaySO;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 支付Controller
 */
@Slf4j
@RestController
@RequestMapping("/pay")
public class PayController extends BaseController {

    @Autowired
    private PayService payService;


    /**
     * 支付宝二维码支付
     * @param aliPaySO
     * @return
     */
    @PostMapping(value = "/ali/web",produces = {"application/json"})
    public ResultMessage aliWebPay(@RequestBody AliPaySO aliPaySO){
        UserInfo userInfo = this.getLoginUser();

        if(this.restriction(userInfo.getUserId())){
            return new ResultMessage(ResultMessage.FAILED_CODE,"操作过于频繁，请稍后再试！");
        }

        return payService.aliWebPay(userInfo.getUserId(),aliPaySO.getOrderNos(),Double.parseDouble(aliPaySO.getAmount()));
    }

    /**
     * 支付宝网页充值回调
     */
    @RequestMapping(value = "/alipay/back")
    public void aliPayWebBack(){
        payService.aliPayWebBack(this.getRequest());
    }


    /**
     * 微信二维码支付
     * @param weChatPaySO
     * @return
     */
    @PostMapping(value = "/wechat/web",produces = {"application/json"})
    public ResultMessage weChatWebPay(@RequestBody WeChatPaySO weChatPaySO){
        UserInfo userInfo = this.getLoginUser();

        if(this.restriction(userInfo.getUserId())){
            return new ResultMessage(ResultMessage.FAILED_CODE,"操作过于频繁，请稍后再试！");
        }

        String str = null;
        try{
            str = payService.weChatWebPay(userInfo.getUserId(),weChatPaySO.getOrderNos(),weChatPaySO.getAmount());
        }catch (Exception e){
            e.printStackTrace();
        }
        if(str == null){
            return new ResultMessage(ResultMessage.FAILED_CODE,"暂未开放");
        }else {
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,str);
        }

    }


    /**
     * 微信支付通知
     * @return
     */
    @PostMapping(value = "/wechat/notify",produces = {"application/json"})
    public ResultMessage weChatPayNotify(HttpServletRequest request, HttpServletResponse response){
        String str = null;
        payService.weChatPayNotify(request,response);
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,str);
    }



}
