package com.core.manycloudservice.service;

import com.core.manycloudcommon.entity.LevelInfo;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.so.user.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public interface UserService {

    /**
     * 用户注册
     * @param registerSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    ResultMessage register(RegisterSO registerSO);

    /**
     * 登录
     * @param loginSO
     * @return
     */
    ResultMessage login(LoginSO loginSO);


    /**
     * 验证手机号是否存在
     * @param email
     * @return
     */
    ResultMessage verifyEamil(String email);

    /**
     * 更新用户信息
     * @param updateUserInfoSO
     * @return
     */
    ResultMessage updateUserInfo(UpdateUserInfoSO updateUserInfoSO);


    /**
     * 更新用户密码
     * @param userId
     * @param newPwd
     * @return
     */
    ResultMessage updateUserPwd(String userId,String newPwd);


    /**
     * 分页查询用户操作日志
     * @param userId
     * @param queryUserLogListSO
     * @return
     */
    ResultMessage queryUserLogList(String userId, QueryUserLogListSO queryUserLogListSO);


    /**
     * 获取用户余额
     * @param userId
     * @return
     */
    BigDecimal getBalance(String userId);


    /***
     * 查询推广链接(客户端)
     * @param userId
     * @return
     */
    String getCommissionUrl(String userId);


    /**
     * 查询用户系统消息列表
     * @param userId
     * @param querySysLogListSO
     * @return
     */
    ResultMessage querySysLogList(String userId, QuerySysLogListSO querySysLogListSO);


    /**
     * 查询用户未读的系统消息数量
     * @param userId
     * @return
     */
    Integer querySysLogUnreadNum(String userId);


    /**
     * 更新系统消息已读
     * @param updateSysLogUnreadSO
     * @return
     */
    ResultMessage updateSysLogUnread(UpdateSysLogUnreadSO updateSysLogUnreadSO);


    /**
     * 获取用户下一级数据
     * @param userId
     * @return
     */
    ResultMessage getLevel(String userId);

    /**
     * 解析用户VIP等级
     * @param userId
     * @return
     */
    LevelInfo analysisLevel(String userId);

}
