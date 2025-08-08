package com.core.manycloudservice.controller;

import com.core.manycloudcommon.controller.BaseController;
import com.core.manycloudcommon.entity.LevelInfo;
import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.utils.*;
import com.core.manycloudservice.service.UserService;
import com.core.manycloudservice.so.user.*;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;

/**
 * 用户Controller
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;


    /**
     * 注册图形验证码
     *
     * @return
     */
    @GetMapping(value = "/verify/img")
    public String getByte(){
        Object[] objs = VerifyUtil.createImage();
        String randomStr = (String) objs[0];
        HttpSession session = getRequest().getSession();
        log.info("sessionID = {}",session.getId());
        log.info("Verify img code  result: " + randomStr.toUpperCase());
        RedisUtil.setEx("USER:IMG:" + session.getId(), randomStr.toUpperCase(), 600);// 存储到redis中，后续用于作验证
        return Base64.encodeBase64String((byte[]) objs[1]);
    }


    /**
     * 获取注册邮箱验证码
     * @return
     */
    @PostMapping(value = "/verify/login/email",produces = {"application/json"})
    public ResultMessage getLoginPhoneVerify(@RequestBody LoginEmailVerifySO loginEmailVerifySO){
        if(StringUtils.isEmpty(loginEmailVerifySO.getCode())){
            return new ResultMessage(ResultMessage.FAILED_CODE,"请输入图形验证码");
        }
        HttpSession session = getRequest().getSession();
        String redisCode = RedisUtil.get("USER:IMG:" + session.getId());
        if (loginEmailVerifySO.getCode().toUpperCase().equals(redisCode)){
            RedisUtil.del("USER:IMG:" + session.getId());
            String emailCode = CommonUtil.getRandomNumber(6);
            log.info("邮箱[{}],注册验证码---->[{}]",loginEmailVerifySO.getEmail().trim(),emailCode.toUpperCase());
            RedisUtil.setEx("USER:EMAIL:" + loginEmailVerifySO.getEmail().trim(), emailCode.toUpperCase(), 600);// 存储到redis中，后续用于作验证
            JSONObject json = new JSONObject();
            json.put("code",emailCode);
            //SendGmailEmail.sendVerificationCode(loginEmailVerifySO.getEmail(),"【洛特云】注册验证码","验证码:"+emailCode+"，您正在注册成为新用户，感谢您的支持！");
            SendQQMailUtil.send("【洛特云】注册验证码","验证码:"+emailCode+"，您正在注册成为新用户，感谢您的支持！",loginEmailVerifySO.getEmail());
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            log.info("sessionID = {}",session.getId());
            log.info("验证码参数[{}],缓存验证码---->[{}]",loginEmailVerifySO.getCode().toUpperCase().trim(),redisCode);
            return new ResultMessage(ResultMessage.FAILED_CODE,"校验码错误");
        }
    }

    /**
     * 注册
     * @param registerSO
     * @return
     */
    @PostMapping(value = "/register",produces = {"application/json"})
    public ResultMessage register(@RequestBody RegisterSO registerSO){
        registerSO.setIp(this.getIp());

        if(1 == registerSO.getCodeType()){//邮箱验证码校验
            String emailCode1 = RedisUtil.get("USER:EMAIL:" + registerSO.getEmail().trim());
            if (!registerSO.getCode().toUpperCase().equals(emailCode1)) {
                log.info("邮箱[{}]注册校验码错误[{}]------------->[{}]",registerSO.getEmail().trim(),registerSO.getCode(),emailCode1);
                return new ResultMessage(ResultMessage.FAILED_CODE,"校验码错误");
            }else{

                registerSO.setIp(this.getIp());
                ResultMessage r = userService.register(registerSO);
                if(r.getCode().equals(ResultMessage.SUCCEED_CODE)){
                    RedisUtil.del("USER:PHONE:" + registerSO.getEmail().trim());
                }
                return r;
            }
        }else{//公众号验证码校验
            String emailCode2 = RedisUtil.get("USER:WX:R:" + registerSO.getCode());
            if(StringUtils.isNotEmpty(emailCode2)){
                registerSO.setIp(this.getIp());
                registerSO.setOpenId(emailCode2);
                ResultMessage r = userService.register(registerSO);
                if(r.getCode().equals(ResultMessage.SUCCEED_CODE)){
                    RedisUtil.del("USER:WX:R:" + registerSO.getCode());
                    RedisUtil.del("USER:WX:X:" + emailCode2);
                }
                return r;
            }else{
                log.info("注册校验码错误[{}]------------->[{}]",registerSO.getCode(),emailCode2);
                return new ResultMessage(ResultMessage.FAILED_CODE,"校验码错误");
            }
        }
    }

    /**
     * 登录
     * @param loginSO
     * @return
     */
    @PostMapping(value = "/login",produces = {"application/json"})
    public ResultMessage login(@RequestBody LoginSO loginSO){
        loginSO.setIp(this.getIp());
        return userService.login(loginSO);
    }

    /**
     * 退出登录
     * @return
     */
    @GetMapping(value = "/exit",produces = {"application/json"})
    public ResultMessage exit(){
        UserInfo userInfo = this.getLoginUser();
        RedisUtil.del(userInfo.getUserId());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
    }


    /**
     * 找回密码图形验证码
     * @return
     */
    @GetMapping(value = "/upwd/img")
    public String getUpdateByte(){
        Object[] objs = VerifyUtil.createImage();
        String randomStr = (String) objs[0];
        HttpSession session = getRequest().getSession();
        log.info("upwd img code  result: " + randomStr.toUpperCase());
        RedisUtil.setEx("USER:UPWD:" + session.getId(), randomStr.toUpperCase(), 600);// 存储到redis中，后续用于作验证
        return Base64.encodeBase64String((byte[]) objs[1]);
    }


    /**
     * 获取找回密码邮箱验证码
     * @return
     */
    @PostMapping(value = "/verify/upwd/email",produces = {"application/json"})
    public ResultMessage getUpdatePhoneVerify(@RequestBody LoginEmailVerifySO loginEmailVerifySO){
        HttpSession session = getRequest().getSession();

        ResultMessage r = userService.verifyEamil(loginEmailVerifySO.getEmail());
        if(r.getCode().equals(ResultMessage.FAILED_CODE)){
            return new ResultMessage(ResultMessage.FAILED_CODE,"账号未注册");
        }

        String redisCode = RedisUtil.get("USER:UPWD:" + session.getId());
        if (loginEmailVerifySO.getCode().toUpperCase().equals(redisCode)){
            RedisUtil.del("USER:UPWD:" + session.getId());
            String phoneCode = CommonUtil.getRandomNumber(6);
            log.info("邮箱[{}],改密验证码---->[{}]",loginEmailVerifySO.getEmail().trim(),phoneCode.toUpperCase());
            RedisUtil.setEx("UPWD:EMAIL:" + loginEmailVerifySO.getEmail().trim(), phoneCode.toUpperCase(), 600);// 存储到redis中，后续用于作验证
            JSONObject json = new JSONObject();
            json.put("code",phoneCode);
            //SendGmailEmail.sendVerificationCode(loginEmailVerifySO.getEmail(),"【洛特云】改密验证码","您的动态码为："+phoneCode+"，您正在进行密码重置操作，如非本人操作，请忽略本短信！");
            SendQQMailUtil.send("【洛特云】改密验证码","您的动态码为："+phoneCode+"，您正在进行密码重置操作，如非本人操作，请忽略本短信！",loginEmailVerifySO.getEmail());
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"校验码错误");
        }
    }

    /**
     * 找回密码
     * @param updatePwdSO
     * @return
     */
    @PostMapping(value = "/retrieve/pwd",produces = {"application/json"})
    public ResultMessage updatePwd(@RequestBody UpdatePwdSO updatePwdSO){

        ResultMessage vr = userService.verifyEamil(updatePwdSO.getEmail());
        if(ResultMessage.FAILED_CODE.equals(vr.getCode())){
            return new ResultMessage(ResultMessage.FAILED_CODE,"无效邮箱信息");
        }
        UserInfo userInfo = (UserInfo)vr.getData();
        UpdateUserInfoSO updateUserInfoSO = UpdateUserInfoSO.builder()
                .id(userInfo.getId())
                .loginPwd(updatePwdSO.getNewPwd())
                .build();

        if(updatePwdSO.getCodeType() == 1){
            String emailCode = RedisUtil.get("UPWD:EMAIL:" + updatePwdSO.getEmail().trim());
            if (!updatePwdSO.getCode().toUpperCase().equals(emailCode)) {
                log.info("改密[{}]校验码错误[{}]------------->[{}]",updatePwdSO.getEmail().trim(),updatePwdSO.getCode(),emailCode);
                return new ResultMessage(ResultMessage.FAILED_CODE,"改密校验码错误");
            }else{

                ResultMessage r = userService.updateUserPwd(userInfo.getUserId(),updatePwdSO.getNewPwd());
                if(r.getCode().equals(ResultMessage.SUCCEED_CODE)){
                    RedisUtil.del("UPWD:EMAIL:" + updatePwdSO.getEmail().trim());
                }
                return r;
            }
        }else{
            String openId = RedisUtil.get("USER:UPDATE:R:" + updatePwdSO.getCode());
            if(StringUtils.isNotEmpty(openId)){

                ResultMessage r = userService.updateUserInfo(updateUserInfoSO);
                if(r.getCode().equals(ResultMessage.SUCCEED_CODE)){
                    RedisUtil.del("USER:UPDATE:R:" + updatePwdSO.getCode());
                    RedisUtil.del("USER:UPDATE:X:" + openId);
                }
                return r;
            }else{
                log.info("公众号[{}]改密校验码错误------------->[{}]",updatePwdSO.getCode());
                return new ResultMessage(ResultMessage.FAILED_CODE,"改密校验码错误");
            }
        }

    }

    /**
     * 修改用户昵称
     * @param updateUserNameSO
     * @return
     */
    @PostMapping(value = "/update/name",produces = {"application/json"})
    public ResultMessage updateUserName(@RequestBody UpdateUserNameSO updateUserNameSO){
        UserInfo userInfo = this.getLoginUser();
        UpdateUserInfoSO updateUserInfoSO = UpdateUserInfoSO.builder()
                .id(userInfo.getId())
                .nickName(updateUserNameSO.getNickName())
                .build();
        return userService.updateUserInfo(updateUserInfoSO);
    }

    /**
     * 修改用户邮箱
     * @param updateUserEmailSO
     * @return
     */
    @PostMapping(value = "/update/email",produces = {"application/json"})
    public ResultMessage updateUserEmail(@RequestBody UpdateUserEmailSO updateUserEmailSO){
        UserInfo userInfo = this.getLoginUser();
        if(userInfo.getLoginPwd().equals(MD5.MD5Encode(MD5.MD5Encode(MD5.MD5Encode(updateUserEmailSO.getLoginPwd()))))){
            UpdateUserInfoSO updateUserInfoSO = UpdateUserInfoSO.builder()
                    .id(userInfo.getId())
                    .email(updateUserEmailSO.getEmail())
                    .build();
            return userService.updateUserInfo(updateUserInfoSO);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"密码校验失败");
        }

    }


    /**
     * 修改用户密码
     * @param updateUserPwdSO
     * @return
     */
    @PostMapping(value = "/update/pwd",produces = {"application/json"})
    public ResultMessage updateUserPwd(@RequestBody UpdateUserPwdSO updateUserPwdSO){
        UserInfo userInfo = this.getLoginUser();
        if(userInfo.getLoginPwd().equals(MD5.MD5Encode(MD5.MD5Encode(MD5.MD5Encode(updateUserPwdSO.getLoginPwd()))))){
            return userService.updateUserPwd(userInfo.getUserId(),updateUserPwdSO.getNewPwd());
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"密码校验失败");
        }

    }

    /**
     * 查询用户操作日志
     * @param queryUserLogListSO
     * @return
     */
    @PostMapping(value = "/query/log/list",produces = {"application/json"})
    public ResultMessage queryLogList(@RequestBody QueryUserLogListSO queryUserLogListSO){
        UserInfo userInfo = this.getLoginUser();
        return userService.queryUserLogList(userInfo.getUserId(),queryUserLogListSO);
    }


    /**
     * 获取用户余额
     * @return
     */
    @GetMapping(value = "/get/balance",produces = {"application/json"})
    public ResultMessage getBalance(){
        UserInfo userInfo = this.getLoginUser();
        BigDecimal balance = userService.getBalance(userInfo.getUserId());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,balance);
    }


    /***
     * 查询推广链接(客户端)
     * @return
     */
    @GetMapping(value = "/get/commission/url",produces = {"application/json"})
    public ResultMessage getCommissionUrl(){
        UserInfo userInfo = this.getLoginUser();
        String url = userService.getCommissionUrl(userInfo.getUserId());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,url);
    }


    /**
     * 查询用户系统消息列表
     * @param querySysLogListSO
     * @return
     */
    @PostMapping(value = "/query/syslog/list",produces = {"application/json"})
    public ResultMessage querySysLogList(@RequestBody QuerySysLogListSO querySysLogListSO){
        UserInfo userInfo = this.getLoginUser();
        return userService.querySysLogList(userInfo.getUserId(),querySysLogListSO);
    }


    /**
     * 获取用户未读的系统消息数量
     * @return
     */
    @GetMapping(value = "/get/syslog/unreadnum",produces = {"application/json"})
    public ResultMessage querySysLogUnreadNum(){
        UserInfo userInfo = this.getLoginUser();
        Integer num = userService.querySysLogUnreadNum(userInfo.getUserId());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,num);
    }


    /**
     * 更新系统消息已读
     * @param updateSysLogUnreadSO
     * @return
     */
    @PostMapping(value = "/update/syslog/unread",produces = {"application/json"})
    public ResultMessage updateSysLogUnread(@RequestBody UpdateSysLogUnreadSO updateSysLogUnreadSO){
        return userService.updateSysLogUnread(updateSysLogUnreadSO);
    }


    /**
     * 解析用户VIP等级
     * @return
     */
    @GetMapping(value = "/get/level",produces = {"application/json"})
    public ResultMessage getLevel(){
        UserInfo userInfo = this.getLoginUser();
        return userService.getLevel(userInfo.getUserId());
    }


}
