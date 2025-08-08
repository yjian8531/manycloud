package com.core.manycloudtimer.util;


import com.core.manycloudcommon.entity.UserWx;
import com.core.manycloudcommon.mapper.SysParamMapper;
import com.core.manycloudcommon.mapper.UserWxMapper;
import com.core.manycloudcommon.utils.DateUtil;
import com.core.manycloudcommon.utils.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WeiXinCaller {

    @Value("${wxm.apiurl}")
    private String apiUrl;


    @Autowired
    private UserWxMapper userWxMapper;

    @Autowired
    private SysParamMapper sysParamMapper;


    /**
     * 提现到账微信公众号通知
     * @param userId 用户ID
     * @param createTime 创建时间
     * @param account 提现账号
     * @param moneyNum 提现金额
     * @param taxRatio 税点
     * @param moneyTax 到账金额
     */
    public void sendWithdrawalSuccess(String userId,Date createTime,String account,BigDecimal moneyNum,BigDecimal taxRatio,BigDecimal moneyTax){
        try{
            UserWx userWx = userWxMapper.selectByUserId(userId);
            if(userWx != null){
                //获取token
                String accessToken = sysParamMapper.selectByTail("WX_ACCESS_TOKEN").getVal();

                Map<String,Object> data = new HashMap<>();

                Map<String,Object> keyword1 = new HashMap<>();
                keyword1.put("value", DateUtil.dateStr4(createTime));

                Map<String,Object> keyword2 = new HashMap<>();
                keyword2.put("value",account);

                Map<String,Object> keyword3 = new HashMap<>();
                keyword3.put("value", moneyNum.toPlainString()+" CNY");

                Map<String,Object> keyword4 = new HashMap<>();
                keyword4.put("value", taxRatio.toPlainString()+" %");


                Map<String,Object> keyword5 = new HashMap<>();
                keyword5.put("value", moneyTax.toPlainString()+" CNY");

                data.put("keyword1",keyword1);
                data.put("keyword2",keyword2);
                data.put("keyword3",keyword3);
                data.put("keyword4",keyword4);
                data.put("keyword5",keyword5);

                sendMessage(accessToken,userWx.getOpenId(),"4ZoTCgQlBX69BhrqcuUHtLw9O0F2Jf2ILuDLAzPHkKU",data);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 充值成功微信公众号通知
     * @param userId 用户ID
     * @param account 账号
     * @param amount 充值金额
     * @param type 方式(0:在线充值，1:手动操作)
     * @param time 时间
     */
    public void sendRechargeSuccess(String userId,String account,BigDecimal amount,int type,Date time){
        try{
            UserWx userWx = userWxMapper.selectByUserId(userId);
            if(userWx != null){
                //获取token
                String accessToken = sysParamMapper.selectByTail("WX_ACCESS_TOKEN").getVal();

                Map<String,Object> data = new HashMap<>();

                Map<String,Object> keyword1 = new HashMap<>();
                keyword1.put("value", account);

                Map<String,Object> keyword2 = new HashMap<>();
                keyword2.put("value",amount.toPlainString()+" CNY");

                Map<String,Object> keyword3 = new HashMap<>();
                if(type == 0){
                    keyword3.put("value", "在线充值");
                }else{
                    keyword3.put("value", "手动充值");
                }
                Map<String,Object> keyword4 = new HashMap<>();
                keyword4.put("value", DateUtil.dateStr4(time));

                data.put("keyword1",keyword1);
                data.put("keyword2",keyword2);
                data.put("keyword3",keyword3);
                data.put("keyword4",keyword4);

                sendMessage(accessToken,userWx.getOpenId(),"xeH3k2FuIhiJrs-bA-G0yly-jb0BlbTWI2B1CamaBzw",data);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 购买成功微信公众号通知
     * @param userId 用户ID
     * @param productName 产品名称
     * @param price 价格
     * @param time 时间
     */
    public void sendBuySuccess(String userId, String productName, BigDecimal price, Date time){

        try{
            UserWx userWx = userWxMapper.selectByUserId(userId);
            if(userWx != null){
                //获取token
                String accessToken = sysParamMapper.selectByTail("WX_ACCESS_TOKEN").getVal();

                Map<String,Object> data = new HashMap<>();

                Map<String,Object> keyword1 = new HashMap<>();
                keyword1.put("value", productName);

                Map<String,Object> keyword2 = new HashMap<>();
                keyword2.put("value",price.toPlainString());

                Map<String,Object> keyword3 = new HashMap<>();
                keyword3.put("value", DateUtil.dateStr4(time));

                data.put("keyword1",keyword1);
                data.put("keyword2",keyword2);
                data.put("keyword3",keyword3);

                sendMessage(accessToken,userWx.getOpenId(),"00BWpwFQJx-naTEbFxS24uP8gg8ExbQtYdKNtuAecEM",data);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    /**
     * 续费成功微信公众号通知
     * @param userId 用户ID
     * @param productName 产品名称
     * @param price 价格
     * @param endTime 到期时间
     * @param period 续费周期(月)
     */
    public void sendRenewSuccess(String userId, BigDecimal price,String productName, Date endTime,int period){

        try{
            UserWx userWx = userWxMapper.selectByUserId(userId);
            if(userWx != null){
                //获取token
                String accessToken = sysParamMapper.selectByTail("WX_ACCESS_TOKEN").getVal();

                Map<String,Object> data = new HashMap<>();

                Map<String,Object> keyword1 = new HashMap<>();
                keyword1.put("value",price.toPlainString());

                Map<String,Object> keyword2 = new HashMap<>();
                keyword2.put("value", productName);

                Map<String,Object> keyword3 = new HashMap<>();
                keyword3.put("value", DateUtil.dateStr4(endTime));

                Map<String,Object> keyword4 = new HashMap<>();
                keyword4.put("value", period + " 月");

                data.put("keyword1",keyword1);
                data.put("keyword2",keyword2);
                data.put("keyword3",keyword3);
                data.put("keyword4",keyword4);

                sendMessage(accessToken,userWx.getOpenId(),"lMbXYJJaovXOpsbEuMSUfZu1kA_ZqYOlf0jugqmK_4g",data);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }




    /**
     * 待续费微信公众号通知
     * @param userId 用户ID
     * @param userName 用户名
     * @param account 账号
     * @param price 价格
     * @param productName 产品名称
     * @param endTime 到期时间
     */
    public void sendWaitRenew(String userId,String userName, String account,BigDecimal price,String productName, Date endTime){

        try{
            UserWx userWx = userWxMapper.selectByUserId(userId);
            if(userWx != null){
                //获取token
                String accessToken = sysParamMapper.selectByTail("WX_ACCESS_TOKEN").getVal();

                Map<String,Object> data = new HashMap<>();
                Map<String,Object> keyword1 = new HashMap<>();
                keyword1.put("value",userName);

                Map<String,Object> keyword2 = new HashMap<>();
                keyword2.put("value",account);

                Map<String,Object> keyword3 = new HashMap<>();
                if(price != null){
                    keyword3.put("value", productName+" 金额:"+price.toPlainString()+"CNY");
                }else{
                    keyword3.put("value", productName);
                }

                Map<String,Object> keyword4 = new HashMap<>();
                keyword4.put("value", DateUtil.dateStr4(endTime));

                data.put("keyword1",keyword1);
                data.put("keyword2",keyword2);
                data.put("keyword3",keyword3);
                data.put("keyword4",keyword4);
                sendMessage(accessToken,userWx.getOpenId(),"2krESq37a0W9GTFC5OGMaGeSEiqZ5JsHVHm4I9Jv8Rc",data);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    /**
     * 产品到期通知推送
     * @param userId
     * @param account
     * @param renewType 续费类型(0:手动续费,1:自动续费)
     * @param productName 产品名称
     */
    public void sendExpire(String userId, String account,int renewType,String productName){

        try{
            UserWx userWx = userWxMapper.selectByUserId(userId);
            if(userWx != null){
                //获取token
                String accessToken = sysParamMapper.selectByTail("WX_ACCESS_TOKEN").getVal();

                Map<String,Object> data = new HashMap<>();
                Map<String,Object> keyword1 = new HashMap<>();
                keyword1.put("value",account);

                Map<String,Object> keyword2 = new HashMap<>();
                if(renewType == 0){
                    keyword2.put("value","手动续费");
                }else{
                    keyword2.put("value","自动续费");
                }


                Map<String,Object> keyword3 = new HashMap<>();
                keyword3.put("value", productName);

                Map<String,Object> keyword4 = new HashMap<>();
                keyword4.put("value", "已过期(3天后销毁)");

                data.put("keyword1",keyword1);
                data.put("keyword2",keyword2);
                data.put("keyword3",keyword3);
                data.put("keyword4",keyword4);
                sendMessage(accessToken,userWx.getOpenId(),"rNgfhz0Gj64fBLRhp3FwbTWUakLbyvCFYGo5MlbZsQ0",data);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }




    /**
     * 推送微信通知消息
     * @param accessToken token
     * @param openId 用户openId
     * @param templateId 消息模板ID
     * @param data 消息模板数据
     * @return
     * @throws Exception
     */
    private String sendMessage(String accessToken,String openId, String templateId,Map<String,Object> data) throws Exception{
        String api = "/cgi-bin/message/template/send?access_token="+accessToken;

        Map<String,Object> param = new HashMap<>();
        param.put("touser",openId);
        param.put("template_id",templateId);
        param.put("data",data);

        log.info("请求参数---{}",JSONObject.fromObject(param).toString());
        String str = HttpRequest.postJson(apiUrl+api,JSONObject.fromObject(param).toString(),new HashMap<>());
        return str;
    }

}
