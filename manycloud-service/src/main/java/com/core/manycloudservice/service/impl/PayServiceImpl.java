package com.core.manycloudservice.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.enums.SyslogTypeEnum;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.model.SyslogModel;
import com.core.manycloudcommon.utils.*;
import com.core.manycloudservice.service.OrderService;
import com.core.manycloudservice.service.PayService;
import com.core.manycloudservice.util.AliPayUtil;
import com.core.manycloudservice.util.WeChatUtil;
import com.core.manycloudservice.util.WeiXinCaller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 充值业务
 */
@Slf4j
@Service
public class PayServiceImpl implements PayService {


    @Value("${wx.appid}")
    public String appid;

    @Value("${wx.appsecret}")
    public String appsecret;

    @Value("${wx.mchid}")
    public String merId;

    @Value("${wx.paternerkey}")
    public String key;

    @Value("${wx.merchantSerialNumber}")
    public String merchantSerialNumber;

    // 支付回调
    @Value("${wx.h5.notify.url}")
    public String notifyh5;

    @Autowired
    private Environment ev;

    @Autowired
    private TopupInfoMapper topupInfoMapper;

    @Autowired
    private FinanceDetailMapper financeDetailMapper;

    @Autowired
    private UserFinanceMapper userFinanceMapper;

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private WeiXinCaller weiXinCaller;

    @Autowired
    private UserInfoMapper userInfoMapper;

    /**
     * 支付宝网页支付
     * @param userId 用户ID
     * @param amount 充值金额
     * @return
     */
    public ResultMessage aliWebPay(String userId,List<String> orderNos, Double amount){

        //创建交易订单
        TopupInfo topupInfo = new TopupInfo();
        topupInfo.setUserId(userId);
        topupInfo.setTopupNo(CommonUtil.getRandomStr(32));
        topupInfo.setMoneyNum(BigDecimal.valueOf(amount));
        topupInfo.setType(CommonUtil.STATUS_1);//支付宝

        if(orderNos !=null && orderNos.size() > 0){
            StringBuffer str = new StringBuffer();
            for(String orderNo : orderNos){
                str.append(orderNo+",");
            }
            topupInfo.setOrderNo(str.toString().substring(0,str.length() - 1));
            topupInfo.setWay(CommonUtil.STATUS_1);//订单支付
        }else{
            topupInfo.setWay(CommonUtil.STATUS_0);//余额充值
        }

        topupInfo.setCreateTime(new Date());
        topupInfo.setUpdateTime(new Date());




        //实例化客户端
        AlipayClient alipayClient = new DefaultAlipayClient(ev.getProperty("al.web.url"),ev.getProperty("al.appid"), AliPayUtil.getPrivateKey(),
                "json", "UTF-8", AliPayUtil.getPublicKey(), "RSA2");
        //实例化具体API对应的request类,类名称和接口名称对应,当前调用接口名称：alipay.trade.app.pay
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest ();

        //SDK已经封装掉了公共参数，这里只需要传入业务参数。以下方法为sdk的model入参方式(model和biz_content同时存在的情况下取biz_content)。
        //支付提交参数
        Map<String,String> paramMap = new HashMap<String,String>();
        paramMap.put("body","充值");
        paramMap.put("subject","云服务器");
        paramMap.put("out_trade_no",topupInfo.getTopupNo());
        paramMap.put("timeout_express","10m");
        paramMap.put("total_amount",String.format("%.2f", amount));
        paramMap.put("product_code","FAST_INSTANT_TRADE_PAY");
        request.setBizContent(JSONObject.fromObject(paramMap).toString());
        request.setNotifyUrl(ev.getProperty("al.web.notify.url"));//异步回调地址
        request.setReturnUrl(ev.getProperty("al.web.return.url"));//同步回调网址

        try {
            log.info("支付宝网页支付提交参数:{}",JSONObject.fromObject(paramMap).toString());
            //这里和普通的接口调用不同，使用的是sdkExecute
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            log.info("支付宝网页支付提交响应:"+ JSONObject.fromObject(response.getParams()).toString());

            if(response.isSuccess()){
                log.info("支付宝网页支付调用成功:"+ topupInfo.getTopupNo());

                String payBody = response.getBody();//resultStr 可以直接给客户端请求，无需再做处理。

                //进行中状态
                topupInfo.setStatus(0);
                //保存交易订单信息
                topupInfoMapper.insertSelective(topupInfo);
                Map<String,String> resultMap = new HashMap<>();
                resultMap.put("orderNo",topupInfo.getTopupNo());
                resultMap.put("body",payBody);
                return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);

            } else {
                log.info("支付宝网页支付调用失败:"+ topupInfo.getTopupNo());
                //订单交易失败状态
                topupInfo.setStatus(3);
                //保存交易订单信息
                topupInfoMapper.insertSelective(topupInfo);
                return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
            }

        } catch (AlipayApiException e) {
            e.printStackTrace();
            log.info("支付宝网页支付，申请失败:"+e.getMessage());
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG,e.getErrCode());
        }

    }

    /**
     * 支付宝网页支付回调
     * @param request
     */
    @Transactional(rollbackFor = Exception.class)
    public void aliPayWebBack(HttpServletRequest request) {
        log.info("----------------支付宝网页支付回调-------" + request);
        Map<String, String> params = new HashMap<String, String>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            // 乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
            // valueStr = new String(valueStr.getBytes("ISO-8859-1"), "gbk");
            params.put(name, valueStr);
        }
        log.info("回调信息:"+JSONObject.fromObject(params).toString());
        try {
            // 调用SDK验证签名
            boolean flag = AlipaySignature.rsaCheckV1(params, AliPayUtil.getPublicKey(),
                    "UTF-8", "RSA2");
            // 商户订单号
            String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "UTF-8");
            // 支付宝交易号
            String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"), "UTF-8");
            // 交易状态
            String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");

            //获取订单支付回调处理标记
            String leab = RedisUtil.get("ALIPC:PAY:" + out_trade_no);
            if(StringUtils.isNotEmpty(leab)){
                log.error("支付宝手机网页支付回调,支付订单处理中:"+out_trade_no);
                return;
            }

            /** 标记订单支付处理中 **/
            RedisUtil.setEx("ALIPC:PAY:" + out_trade_no,out_trade_no,5000);

            //获取交易订单信息
            TopupInfo topupInfo = topupInfoMapper.selectByNo(out_trade_no);
            if(topupInfo.getStatus() == CommonUtil.STATUS_3 || topupInfo.getStatus() == CommonUtil.STATUS_2){
                log.info("支付宝手机网页交易异步通知-订单已处理[{}]",out_trade_no);
                return;
            }

            if (flag) {

                if(trade_status.equals("TRADE_SUCCESS")){//支付成功
                    log.info("支付宝手机网页交易异步通知-支付成功:"+out_trade_no);

                    try{
                        //更新用户余额信息
                        int i = userFinanceMapper.updateBalanceByUserId(topupInfo.getUserId(),"add",topupInfo.getMoneyNum());

                        if(i > 0){

                            //交易完成状态
                            topupInfo.setStatus(CommonUtil.STATUS_2);
                            //更新交易订单
                            topupInfo.setUpdateTime(new Date());
                            topupInfoMapper.updateByPrimaryKeySelective(topupInfo);

                            //用户余额更新记录
                            UserFinance userFinance = userFinanceMapper.selectByUserId(topupInfo.getUserId());
                            balanceLogMapper.insertChange(topupInfo.getUserId(),"add",topupInfo.getMoneyNum(),userFinance.getValidNum(),"支付宝充值");

                            /** 添加财务账单明细信息 **/
                            //添加账单记录
                            FinanceDetail financeDetail = new FinanceDetail();
                            financeDetail.setUserId(topupInfo.getUserId());
                            financeDetail.setFinanceNo(CommonUtil.getRandomStr(12));
                            financeDetail.setProductNo(topupInfo.getTopupNo());
                            financeDetail.setType(1);//平台类型(0:微信,1:支付宝,2:空中云汇,3:微信境外)
                            financeDetail.setMoneyNum(topupInfo.getMoneyNum());
                            financeDetail.setTag("topup");//充值
                            financeDetail.setDirection(0);//收入
                            financeDetail.setWay(2);//交易方式(0:支付宝,1:微信,2:账号余额)
                            financeDetail.setStatus(CommonUtil.STATUS_1);//完成状态
                            financeDetail.setCreateTime(new Date());
                            financeDetail.setUpdateTime(new Date());
                            financeDetailMapper.insertSelective(financeDetail);

                            String timeStr = DateUtil.dateStr4(topupInfo.getCreateTime());
                            String content = "尊敬的用户，您于 "+timeStr+" 号充值的 "+topupInfo.getMoneyNum().toPlainString()+" 元已成功到账。感谢您的支持！";
                            SysLog sysLog = SyslogModel.output(topupInfo.getUserId(), SyslogTypeEnum.RECHARGE,content);
                            sysLogMapper.insertSelective(sysLog);

                            UserInfo userInfo = userInfoMapper.selectById(topupInfo.getUserId());
                            /** 充值到账微信公众号通知 **/
                            weiXinCaller.sendRechargeSuccess(topupInfo.getUserId(),userInfo.getAccount(),topupInfo.getMoneyNum(),0,financeDetail.getUpdateTime());

                            /** 云服务器订单支付 **/
                            if(StringUtils.isNotEmpty(topupInfo.getOrderNo())){
                                List<String> orderNos = new ArrayList<>();
                                String[] orderArray = topupInfo.getOrderNo().split(",");
                                for(String str : orderArray){
                                    if(StringUtils.isNotEmpty(str)){
                                        orderNos.add(str);
                                    }
                                }
                                /** 购买云主机 **/
                                orderService.buy(topupInfo.getUserId(),orderNos,topupInfo.getMoneyNum());
                            }
                        }


                    }catch (Exception e){
                        e.printStackTrace();
                        log.error("支付订单[{}],支付回调更新订单状态失败。--{}",out_trade_no,e.getMessage());
                    }


                }else if(trade_status.equals("TRADE_CLOSED")){//支付失败
                    log.info("支付宝手机网页交易异步通知-支付失败:"+out_trade_no);
                    //交易失败状态
                    topupInfo.setStatus(CommonUtil.STATUS_3);
                    //更新交易订单
                    topupInfo.setUpdateTime(new Date());
                    topupInfoMapper.updateByPrimaryKeySelective(topupInfo);

                }else{//其它触发通知不做处理
                    log.info("支付宝手机网页交易异步通知-"+out_trade_no+"-触发状态:"+trade_status);
                }


            }else{
                log.error("支付宝手机网页支付异步回调，签名错误:"+JSONObject.fromObject(requestParams).toString());
            }



            /** 删除订单支付处理标记 **/
            RedisUtil.del("ALIPC:PAY:" + out_trade_no);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



    /**
     * 微信二维码支付
     * @param money 金额
     * @return url
     */
    public String weChatWebPay(String userId,List<String> orderNos,String money)throws Exception{

        String out_trade_no = CommonUtil.getRandomStr(32);
        Double decimal = Double.parseDouble(money) * 100;
        Integer amount  = decimal.intValue();

        //创建交易订单
        TopupInfo topupInfo = new TopupInfo();
        topupInfo.setUserId(userId);
        topupInfo.setTopupNo(out_trade_no);
        topupInfo.setMoneyNum(new BigDecimal(money));
        topupInfo.setType(CommonUtil.STATUS_1);//支付宝

        if(orderNos != null && orderNos.size() > 0){
            StringBuffer str = new StringBuffer();
            for(String orderNo : orderNos){
                str.append(orderNo+",");
            }
            topupInfo.setOrderNo(str.toString().substring(0,str.length() - 1));
            topupInfo.setWay(CommonUtil.STATUS_1);//订单支付
        }else{
            topupInfo.setWay(CommonUtil.STATUS_0);//余额充值
        }

        topupInfo.setCreateTime(new Date());
        topupInfo.setUpdateTime(new Date());

        HttpPost httpPost = new HttpPost("https://api.mch.weixin.qq.com/v3/pay/transactions/native");
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-type","application/json; charset=utf-8");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper();



        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("mchid",merId)
                .put("appid", appid)
                .put("description", "充值")
                .put("notify_url", notifyh5)
                .put("out_trade_no", out_trade_no);
        rootNode.putObject("amount")
                .put("total", amount);

        objectMapper.writeValue(bos, rootNode);

        httpPost.setEntity(new StringEntity(bos.toString("UTF-8"), "UTF-8"));
        CloseableHttpClient httpClient = WeChatUtil.createHttpClient(merId,merchantSerialNumber,key);
        CloseableHttpResponse response = httpClient.execute(httpPost);

        String bodyAsString = EntityUtils.toString(response.getEntity());
        JSONObject body = JSONObject.fromObject(bodyAsString);
        //订单交易中状态
        topupInfo.setStatus(CommonUtil.STATUS_0);
        //保存交易订单信息
        topupInfoMapper.insertSelective(topupInfo);
        if(body.get("code_url") != null){
            return body.getString("code_url");
        }else{
            return bodyAsString;
        }

    }


    /**
     * 微信支付回调
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void weChatPayNotify(HttpServletRequest request, HttpServletResponse response){

        StringBuffer sb = new StringBuffer();

        try (
                ServletInputStream inputStream = request.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        ) {
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            log.error("读取数据流异常:{}", e);
            return;
        }
        //获取报文
        String body = sb.toString();
        log.info("----------------微信支付回调-------" + body);

        //随机串
        String nonceStr = request.getHeader("Wechatpay-Nonce");

        //微信传递过来的签名
        String signature = request.getHeader("Wechatpay-Signature");

        //证书序列号（微信平台）
        String serialNo = request.getHeader("Wechatpay-Serial");

        //时间戳
        String timestamp = request.getHeader("Wechatpay-Timestamp");

        //应答时间戳\n
        //应答随机串\n
        //应答报文主体\n
        String signStr = Stream.of(timestamp, nonceStr, body).collect(Collectors.joining("\n", "", "\n"));

        try {
            Verifier verifier = WeChatUtil.getVerifier(merId,merchantSerialNumber,key);
            boolean result = verifier.verify(serialNo, signStr.getBytes("utf-8"), signature);
            if(result){
                String plainBody  = WeChatUtil.decryptBody(body,key);
                Map<String,String> map = WeChatUtil.convertWechatPayMsgToMap(plainBody);
                //商户订单号
                String orderId = map.get("out_trade_no");
                //交易状态
                String state = map.get("trade_state");
                //附加数据
                //String accountNo = map.get("account_no");

                //获取订单支付回调处理标记
                String leab = RedisUtil.get("WCIPC:PAY:" + orderId);
                if(StringUtils.isNotEmpty(leab)){
                    log.error("微信支付回调,支付订单处理中:"+orderId);
                    return;
                }

                /** 标记订单支付处理中 **/
                RedisUtil.setEx("WCIPC:PAY:" + orderId,orderId,5000);

                //获取交易订单信息
                TopupInfo topupInfo = topupInfoMapper.selectByNo(orderId);
                if("SUCCESS".equals(state.toUpperCase())){

                    if(topupInfo.getStatus() == CommonUtil.STATUS_3 || topupInfo.getStatus() == CommonUtil.STATUS_2){
                        log.info("微信交易异步通知-订单已处理[{}]",orderId);
                        return;
                    }

                    log.info("微信交易异步通知-支付成功:"+orderId);

                    try{

                        //更新用户余额信息
                        int i = userFinanceMapper.updateBalanceByUserId(topupInfo.getUserId(),"add",topupInfo.getMoneyNum());

                        if(i > 0){

                            //交易完成状态
                            topupInfo.setStatus(CommonUtil.STATUS_2);
                            //更新交易订单
                            topupInfo.setUpdateTime(new Date());
                            topupInfoMapper.updateByPrimaryKeySelective(topupInfo);

                            //用户余额更新记录
                            UserFinance userFinance = userFinanceMapper.selectByUserId(topupInfo.getUserId());
                            balanceLogMapper.insertChange(topupInfo.getUserId(),"add",topupInfo.getMoneyNum(),userFinance.getValidNum(),"支付宝充值");

                            /** 添加财务账单明细信息 **/
                            //添加账单记录
                            FinanceDetail financeDetail = new FinanceDetail();
                            financeDetail.setUserId(topupInfo.getUserId());
                            financeDetail.setFinanceNo(CommonUtil.getRandomStr(12));
                            financeDetail.setProductNo(topupInfo.getTopupNo());
                            financeDetail.setType(0);//平台类型(0:微信,1:支付宝,2:空中云汇,3:微信境外)
                            financeDetail.setMoneyNum(topupInfo.getMoneyNum());
                            financeDetail.setTag("topup");//充值
                            financeDetail.setDirection(0);//收入
                            financeDetail.setWay(2);//交易方式(0:支付宝,1:微信,2:账号余额)
                            financeDetail.setStatus(CommonUtil.STATUS_1);//完成状态
                            financeDetail.setCreateTime(new Date());
                            financeDetail.setUpdateTime(new Date());
                            financeDetailMapper.insertSelective(financeDetail);

                            String timeStr = DateUtil.dateStr4(topupInfo.getCreateTime());
                            String content = "尊敬的用户，您于 "+timeStr+" 号充值的 "+topupInfo.getMoneyNum().toPlainString()+" 元已成功到账。感谢您的支持！";
                            SysLog sysLog = SyslogModel.output(topupInfo.getUserId(), SyslogTypeEnum.RECHARGE,content);
                            sysLogMapper.insertSelective(sysLog);

                            UserInfo userInfo = userInfoMapper.selectById(topupInfo.getUserId());
                            /** 充值到账微信公众号通知 **/
                            weiXinCaller.sendRechargeSuccess(topupInfo.getUserId(),userInfo.getAccount(),topupInfo.getMoneyNum(),0,financeDetail.getUpdateTime());


                            /** 云服务器订单支付 **/
                            if(StringUtils.isNotEmpty(topupInfo.getOrderNo())){
                                try{
                                    List<String> orderNos = new ArrayList<>();
                                    String[] orderArray = topupInfo.getOrderNo().split(",");
                                    for(String str : orderArray){
                                        if(StringUtils.isNotEmpty(str)){
                                            orderNos.add(str);
                                        }
                                    }
                                    /** 购买云主机 **/
                                    orderService.buy(topupInfo.getUserId(),orderNos,topupInfo.getMoneyNum());
                                }catch (Exception e){
                                    e.printStackTrace();
                                }

                            }
                        }


                    }catch (Exception e){
                        e.printStackTrace();
                        log.error("微信支付订单[{}],支付回调更新订单状态失败。--{}",orderId,e.getMessage());
                    }
                }else if("USERPAYING".equals(state.toUpperCase())){//支付中


                }else{//支付失败
                    log.info("微信交易异步通知-支付失败:"+orderId);
                    //交易失败状态
                    topupInfo.setStatus(CommonUtil.STATUS_3);
                    //更新交易订单
                    topupInfo.setUpdateTime(new Date());
                    topupInfoMapper.updateByPrimaryKeySelective(topupInfo);
                    log.info("微信交易异步通知-"+orderId+"-触发状态:"+state);
                }

                /** 删除订单支付处理标记 **/
                RedisUtil.del("WCIPC:PAY:" + orderId);
            }



        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
