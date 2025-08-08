package com.core.manycloudservice.controller;

import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.entity.UserWx;
import com.core.manycloudcommon.mapper.UserInfoMapper;
import com.core.manycloudcommon.mapper.UserWxMapper;
import com.core.manycloudcommon.utils.*;
import com.core.manycloudservice.model.Message;
import com.core.manycloudservice.so.user.BindingSO;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/wx")
public class WxMessageController {


    private String token = "6b50d8318b2a4270bae2300a9ac48daa";

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserWxMapper userWxMapper;


    public static void main(String[] args) throws Exception{
        String accessToken = getAccessToken("wx3268be6fdfff1bc3","283c04b81f2cd70a440fd6743c61b333");
        System.out.println(accessToken);
        /*String accessToken = "89_V356gHk9-n_wM7LhkBMMICzZ6UVEWsfnAbPwtoCGl3PQ5b9nQ99LtVnMIe0hm6AkXeK5CSdm3scPTgsaZQmSmJwrpRgdy9wwOevfLQ6W6ekQKf9_eIifdoR6kLAANMdAHALYX";
        String url = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token="+accessToken;
        String param = "{\"button\":[{\"name\":\"验证码\",\"sub_button\":[{\"type\":\"click\",\"name\":\"短信验证码\",\"key\":\"get_register_code\"},{\"type\":\"click\",\"name\":\"改密验证码\",\"key\":\"get_updatepwd_code\"}]},{\"name\":\"操作\",\"sub_button\":[{\"type\":\"click\",\"name\":\"绑定账号\",\"key\":\"binding_account\"}]},{\"name\":\"帮助中心\",\"sub_button\":[{\"type\":\"click\",\"name\":\"客服信息\",\"key\":\"customer_1_1\"},{\"type\":\"click\",\"name\":\"注意事项\",\"key\":\"attention_1_1\"}]}]}";
        String str = HttpRequest.postJson(url,param,null);
        System.out.println(str);*/
    }

    /**
     * 获取微信公众号token
     * @param appid
     * @param secret
     * @return
     */
    public static String getAccessToken(String appid,String secret){
        String api = "/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
        api = api.replace("{appid}",appid).replace("{secret}",secret);
        String str = HttpRequest.sendGet("https://api.weixin.qq.com"+api);
        JSONObject result = JSONObject.fromObject(str);
        if(result.get("access_token") != null){
            return result.getString("access_token");
        }else{
            log.info("微信公众号获取AccessToken失败:{}",str);
            return null;
        }
    }


    /**
     * 获取注册验证码
     * @param openid
     * @return
     */
    public String getVerifyCode(String openid){
        String registerCode = RedisUtil.get("USER:WX:X:" + openid);
        if(StringUtils.isEmpty(registerCode)){
            registerCode = CommonUtil.getRandomNumber(6);
            log.info("OpenId[{}],注册验证码---->[{}]",openid,registerCode);
            RedisUtil.setEx("USER:WX:R:" + registerCode,openid, 600);// 存储到redis中，后续用于作验证
            RedisUtil.setEx("USER:WX:X:" + openid,registerCode, 600);// 存储到redis中，后续用于作验证
        }
        return registerCode;
    }


    /**
     * 获取改密验证码
     * @param openid
     * @return
     */
    public String getUpdateCode(String openid){
        String registerCode = RedisUtil.get("USER:UPDATE:X:" + openid);
        if(StringUtils.isEmpty(registerCode)){
            registerCode = CommonUtil.getRandomNumber(6);
            log.info("OpenId[{}],改密证码---->[{}]",openid,registerCode);
            RedisUtil.setEx("USER:UPDATE:R:" + registerCode,openid, 600);// 存储到redis中，后续用于作验证
            RedisUtil.setEx("USER:UPDATE:X:" + openid,registerCode, 600);// 存储到redis中，后续用于作验证
        }
        return registerCode;
    }


    @GetMapping(value = "/base")
    public String verify(@RequestParam("signature") String signature, @RequestParam("timestamp")String timestamp,
                         @RequestParam("nonce")String nonce, @RequestParam("echostr")String echostr ){

        log.info("校验参数:{signature:{},timestamp:{},nonce:{},echostr:{}}",signature,timestamp,nonce,echostr);
        if(checkSignature( signature,  timestamp,  nonce) ){
            return echostr;
        }else{
            return "error";
        }
    }

    /**
     * 消息处理
     * @param requestMessage 请求消息
     * @return 响应消息或者“success”
     */
    //改成PostMapping用来接收POST请求，produces指定响应的类型为xml，RequestBody和实体类Message的Xml注解一起实现直接接收xml请求
    @PostMapping(value="/base",produces = MediaType.APPLICATION_XML_VALUE)
    public Object message(@RequestBody Message requestMessage){
        log.info("post方法入参："+requestMessage);
        String fromUserName = requestMessage.getFromUserName();
        String toUserName = requestMessage.getToUserName();


        //新建一个响应对象
        Message responseMessage = new Message();
        //消息来自谁
        responseMessage.setFromUserName(toUserName);
        //消息发送给谁
        responseMessage.setToUserName(fromUserName);

        if(requestMessage.getMsgType().equals("event")){//事件
            if("subscribe".equals(requestMessage.getEvent())){//订阅
                //订阅自动回复消息内容
                responseMessage.setContent("非常感谢您关注洛特云公众号，您可以通过下方\"验证码-注册验证码\"按钮获取注册验证码。也可以通过下方 \"操作-绑定账号\" 来同步接收平台站内消息提示！");
                //消息类型，返回的是文本
                responseMessage.setMsgType("text");
            }else if("CLICK".equals(requestMessage.getEvent())){//菜单按钮

                if("get_register_code".equals(requestMessage.getEventKey())){//注册验证码
                    String code = getVerifyCode(fromUserName);
                    responseMessage.setContent("【洛特云】验证码:"+code+"，您正在注册成为新用户，验证码有效期10分钟。感谢您的支持！");
                    responseMessage.setMsgType("text");
                }else if("get_updatepwd_code".equals(requestMessage.getEventKey())){//改密验证码
                    String code = getUpdateCode(fromUserName);
                    responseMessage.setContent("【洛特云】改密验证码,您的动态码为："+code+"，您正在进行密码重置操作，如非本人操作，请忽略本短信！");
                    responseMessage.setMsgType("text");
                }else if("binding_account".equals(requestMessage.getEventKey())){//绑定操作
                    //消息类型，返回的是链接
                    responseMessage.setMsgType("text");
                    responseMessage.setContent("<a href=\"https://www.lotvps.com/bd/index.html?code="+fromUserName+"\">点我进行绑定校验</a>");
                }else if("customer_1_1".equals(requestMessage.getEventKey())){//客服信息
                    //消息类型，返回的是链接
                    responseMessage.setMsgType("text");
                    responseMessage.setContent("<a href=\"https://www.lotvps.com/h5/index.html\">点我获取客服信息</a>");
                }else if("attention_1_1".equals(requestMessage.getEventKey())){//注意事项
                    responseMessage.setContent("售后人工服务时间是9:30-12：00，13：30-19:00 ，晚上有事可留言客服，第二天上班我们会及时处理！");
                    responseMessage.setMsgType("text");
                }else{
                    responseMessage.setContent("非常抱歉！无法理解您的需求呢。");
                    //消息类型，返回的是文本
                    responseMessage.setMsgType("text");
                }
            }
        }

        //消息创建时间，当前时间就可以
        responseMessage.setCreateTime(System.currentTimeMillis());
        return responseMessage;
    }


    private  boolean checkSignature(String signature, String timestamp, String nonce) {
        String[] tmpArr = {token, timestamp, nonce};
        Arrays.sort(tmpArr);
        String tmpStr = String.join("", tmpArr);
        tmpStr = sha1(tmpStr);

        return tmpStr.equals(signature);
    }

    private  String sha1(String input) {
        try {
            java.security.MessageDigest sha1 = java.security.MessageDigest.getInstance("SHA-1");
            byte[] bytes = sha1.digest(input.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 绑定
     * @param bindingSO
     * @return
     */
    @PostMapping(value = "/binding",produces = {"application/json"})
    public ResultMessage binding(@RequestBody BindingSO bindingSO){

        UserInfo userInfo = userInfoMapper.selectByAccount(bindingSO.getAccount());
        if(userInfo != null){

            if(MD5.MD5Encode(MD5.MD5Encode(MD5.MD5Encode(bindingSO.getLogPwd()))).equals(userInfo.getLoginPwd())){

                UserWx userWx = userWxMapper.selectByUserId(userInfo.getUserId());
                if(userWx == null){
                    userWx = new UserWx();
                    userWx.setUserId(userInfo.getUserId());
                    userWx.setOpenId(bindingSO.getCode());
                    userWx.setStatus(0);
                    userWx.setCreateTime(new Date());
                    int i = userWxMapper.insertSelective(userWx);
                    if(i > 0 ){
                        return new ResultMessage(ResultMessage.SUCCEED_CODE,"绑定成功");
                    }else{
                        return new ResultMessage(ResultMessage.FAILED_CODE,"绑定失败");
                    }

                }else{
                    userWx.setOpenId(bindingSO.getCode());
                    userWx.setStatus(0);
                    int i = userWxMapper.updateByPrimaryKeySelective(userWx);
                    if(i > 0 ){
                        return new ResultMessage(ResultMessage.SUCCEED_CODE,"绑定成功");
                    }else{
                        return new ResultMessage(ResultMessage.FAILED_CODE,"绑定失败");
                    }
                }

            }else{
                return new ResultMessage(ResultMessage.FAILED_CODE,"登录密码错误");
            }

        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"无效账号信息");
        }
    }

}
