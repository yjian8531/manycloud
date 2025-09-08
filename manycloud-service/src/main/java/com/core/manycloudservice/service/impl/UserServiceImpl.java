package com.core.manycloudservice.service.impl;

import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.utils.*;
import com.core.manycloudservice.service.UserService;
import com.core.manycloudservice.so.user.*;
import com.core.manycloudservice.vo.UserLevelVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    public Environment env;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserLogMapper userLogMapper;

    @Autowired
    private UserFinanceMapper userFinanceMapper;

    @Autowired
    private UserProMapper userProMapper;

    @Autowired
    private UserWxMapper userWxMapper;

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private FinanceDetailMapper financeDetailMapper;

    @Autowired
    private LevelInfoMapper levelInfoMapper;

    @Autowired
    private UserLevelMapper userLevelMapper;

    @Autowired
    private UserMedalMapper userMedalMapper;

    @Autowired
    private MedalInfoMapper medalInfoMapper;

    @Autowired
    private VoucherInfoMapper voucherInfoMapper;

    @Autowired
    private VoucherBindMapper voucherBindMapper;

    @Autowired
    private VoucherProductMapper voucherProductMapper;


    /**
     * 用户注册
     * @param registerSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage register(RegisterSO registerSO){

        UserInfo userInfo = userInfoMapper.selectByAccount(registerSO.getEmail());
        if(userInfo != null){
            return new ResultMessage(ResultMessage.FAILED_CODE,"当前邮箱已注册");
        }


        /** 用户填写推广码注册 **/
        UserInfo proUser = null;
        if(StringUtils.isNotEmpty(registerSO.getMarket())){
            proUser = queryMarket(registerSO.getMarket());
            if(proUser == null){
                return new ResultMessage(ResultMessage.FAILED_CODE,"无效推广码");
            }
        }

        userInfo = new UserInfo();
        userInfo.setUserId(CommonUtil.getRandomStr(32));
        userInfo.setAccount(registerSO.getEmail());
        userInfo.setPhone(registerSO.getPhone());
        userInfo.setEmail(registerSO.getEmail());
        String marketNew =  CommonUtil.getRandomStr(6).toUpperCase();
        /** 校验推广码是否存在 **/
        UserInfo marketUser = queryMarket(marketNew);
        while (marketUser!= null){
            marketNew =  CommonUtil.getRandomStr(6).toUpperCase();
            marketUser = queryMarket(marketNew);
        }
        userInfo.setMarket(marketNew);
        userInfo.setNickName(registerSO.getName());
        userInfo.setLoginPwd(MD5.MD5Encode(MD5.MD5Encode(MD5.MD5Encode(registerSO.getPwd()))));
        userInfo.setStatus(CommonUtil.STATUS_0);
        userInfo.setType(CommonUtil.STATUS_0);
        userInfo.setMarket(CommonUtil.getRandomStr(6).toUpperCase());
        userInfo.setUpdateTime(new Date());
        userInfo.setCreateTime(new Date());
        userInfo.setLoginIp(registerSO.getIp());
        int i = userInfoMapper.insertSelective(userInfo);
        if(i > 0){

            if(StringUtils.isNotEmpty(registerSO.getMarket())){
                /** 添加推广信息 **/
                UserPro userPro = new UserPro();
                userPro.setUserId(userInfo.getUserId());
                userPro.setProUserId(proUser.getUserId());
                userPro.setStatus(CommonUtil.STATUS_0);
                userPro.setCreateTime(new Date());
                userProMapper.insertSelective(userPro);

                /** 代金券赠送校验 **/
                checkVoucherBind(userInfo.getUserId(),proUser.getUserId());
            }

            if(StringUtils.isNotEmpty(registerSO.getOpenId())){
                /** 添加微信绑定信息 **/
                UserWx userWx = new UserWx();
                userWx.setUserId(userInfo.getUserId());
                userWx.setOpenId(registerSO.getOpenId());
                userWx.setStatus(CommonUtil.STATUS_0);
                userWx.setCreateTime(new Date());
                userWxMapper.insertSelective(userWx);
            }

            //创建用户财务信息
            UserFinance userFinance = new UserFinance();
            userFinance.setUserId(userInfo.getUserId());
            userFinance.setType(CommonUtil.STATUS_0);
            userFinanceMapper.insertSelective(userFinance);


            /** 缓存用户登录信息到redis **/
            Map<String,Object> map = new HashMap<>();
            String str = CommonUtil.getRandomStr(4);
            map.put("userInfo",userInfo);
            map.put("str",str);
            RedisUtil.setEx(userInfo.getUserId(), JSONObject.fromObject(map).toString(),10800);
            userInfo.setToken(InterceptorUtil.getToken(userInfo.getUserId(),str));

            return new ResultMessage(ResultMessage.SUCCEED_CODE,"注册成功",userInfo);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"注册失败");
        }

    }

    /**
     * 根据推广码查询用户信息
     * @param market
     * @return
     */
    private UserInfo queryMarket(String market){
        UserInfo userInfo = userInfoMapper.selectByMarket(market);
        return userInfo;
    }

    /**
     * 验证邮箱是否存在
     * @param email
     * @return
     */
    public ResultMessage verifyEamil(String email){
        UserInfo userInfo = userInfoMapper.selectByAccount(email);
        if(userInfo == null){
            return new ResultMessage(ResultMessage.FAILED_CODE,"邮箱不存在");
        }else{
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"邮箱存在",userInfo);
        }
    }


    /**
     * 登录
     * @param loginSO
     * @return
     */
    public ResultMessage login(LoginSO loginSO){
        UserInfo userInfo = userInfoMapper.selectByAccount(loginSO.getEmail());
        if(userInfo != null){

            if(userInfo.getStatus() == 1){
                return new ResultMessage(ResultMessage.FAILED_CODE,"当前账号已被禁用");
            }

            if(MD5.MD5Encode(MD5.MD5Encode(MD5.MD5Encode(loginSO.getPwd()))).equals(userInfo.getLoginPwd())){

                /** 缓存用户登录信息到redis **/
                Map<String,Object> map = new HashMap<>();
                String str = CommonUtil.getRandomStr(4);
                map.put("userInfo",userInfo);
                map.put("str",str);
                RedisUtil.setEx(userInfo.getUserId(), JSONObject.fromObject(map).toString(),10800);

                userInfo.setToken(InterceptorUtil.getToken(userInfo.getUserId(),str));

                userInfo.setLoginTime(new Date());
                userInfo.setLoginIp(loginSO.getIp());
                userInfoMapper.updateByPrimaryKeySelective(userInfo);


                UserWx userWx = userWxMapper.selectByUserId(userInfo.getUserId());
                if(userWx == null){
                    userInfo.setWx(0);
                }else{
                    userInfo.setWx(1);
                }
                return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,userInfo);
            }else{
                return new ResultMessage(ResultMessage.FAILED_CODE,"密码错误");
            }
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"账号错误");
        }
    }

    /**
     * 更新用户信息
     * @param updateUserInfoSO
     * @return
     */
    public ResultMessage updateUserInfo(UpdateUserInfoSO updateUserInfoSO){
        UserInfo userInfo = new UserInfo();
        userInfo.setId(updateUserInfoSO.getId());
        userInfo.setAccount(updateUserInfoSO.getEmail());
        userInfo.setEmail(updateUserInfoSO.getEmail());
        userInfo.setNickName(updateUserInfoSO.getNickName());
        userInfo.setPhone(updateUserInfoSO.getPhone());
        userInfo.setType(updateUserInfoSO.getType());
        userInfo.setStatus(updateUserInfoSO.getStatus());
        userInfo.setRemark(updateUserInfoSO.getRemark());
        int i = userInfoMapper.updateByPrimaryKeySelective(userInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"操作成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"操作失败");
        }
    }

    /**
     * 更新用户密码
     * @param userId
     * @param newPwd
     * @return
     */
    public ResultMessage updateUserPwd(String userId,String newPwd){
        UserInfo userInfo = userInfoMapper.selectById(userId);
        userInfo.setLoginPwd(MD5.MD5Encode(MD5.MD5Encode(MD5.MD5Encode(newPwd))));
        int i = userInfoMapper.updateByPrimaryKeySelective(userInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"操作成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"操作失败");
        }
    }


    /**
     * 分页查询用户操作日志
     * @param userId
     * @param queryUserLogListSO
     * @return
     */
    public ResultMessage queryUserLogList(String userId, QueryUserLogListSO queryUserLogListSO){
        PageHelper.startPage(queryUserLogListSO.getPage(), queryUserLogListSO.getPageSize());

        Map<String,Object> param = new HashMap<>();
        param.put("userId",userId);
        param.put("alias",queryUserLogListSO.getAlias());
        Page<UserLog> page = (Page<UserLog>)userLogMapper.selectList(param);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }

    /**
     * 获取用户余额
     * @param userId
     * @return
     */
    public BigDecimal getBalance(String userId){
        UserFinance userFinance = userFinanceMapper.selectByUserId(userId);
        return userFinance.getValidNum();
    }

    /***
     * 查询推广链接(客户端)
     * @param userId
     * @return
     */
    public String getCommissionUrl(String userId){
        UserInfo userInfo = userInfoMapper.selectById(userId);
        String url = env.getProperty("register.market.url");
        url = url.replace("{0}",userInfo.getMarket());
        return url;
    }

    /**
     * 查询用户系统消息列表
     * @param userId
     * @param querySysLogListSO
     * @return
     */
    public ResultMessage querySysLogList(String userId,QuerySysLogListSO querySysLogListSO){
        PageHelper.startPage(querySysLogListSO.getPage(), querySysLogListSO.getPageSize());

        Page<SysLog> page = (Page<SysLog>)sysLogMapper.selectByUser(userId,querySysLogListSO.getStatus());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }

    /**
     * 查询用户未读的系统消息数量
     * @param userId
     * @return
     */
    public Integer querySysLogUnreadNum(String userId){
        Integer num = sysLogMapper.selectUnreadByUser(userId);
        return num;
    }

    /**
     * 更新系统消息已读
     * @param updateSysLogUnreadSO
     * @return
     */
    public ResultMessage updateSysLogUnread(UpdateSysLogUnreadSO updateSysLogUnreadSO){
        SysLog sysLog = new SysLog();
        sysLog.setId(updateSysLogUnreadSO.getId());
        sysLog.setStatus(1);//已读状态
        int i = sysLogMapper.updateByPrimaryKeySelective(sysLog);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"操作成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"操作失败");
        }
    }

    /**
     * 获取用户下一级数据
     * @param userId
     * @return
     */
    public ResultMessage getLevel(String userId){
        /** 获取用户等级信息 **/
        LevelInfo levelInfo =  analysisLevel(userId);
        BigDecimal discount = levelInfo.getDiscount();

        /** 查询用户总消费 **/
        BigDecimal consumption = financeDetailMapper.selectConsumptionByUser(userId);

        /** 下一级信息 **/
        LevelInfo nextLevel = levelInfoMapper.selectByLevel(levelInfo.getLevel() + 1);

        String medalName = null;
        UserMedal userMedal = userMedalMapper.selectByUserId(userId);
        if(userMedal != null){
            MedalInfo medalInfo = medalInfoMapper.selectByPrimaryKey(userMedal.getMedalId());
            medalName = medalInfo.getName();
        }

        UserLevelVO userLevelVO = UserLevelVO.builder()
                .consumption(consumption)
                .discount(discount.compareTo(BigDecimal.valueOf(1)) < 0 ? discount.multiply(BigDecimal.valueOf(10)).toPlainString()+"折" : "无")
                .level(levelInfo.getLevel())
                .levelName(levelInfo.getName())
                .requirement(levelInfo.getRequirement())
                .nextRequirement(nextLevel.getRequirement())
                .medalName(medalName)
                .build();
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,userLevelVO);
    }


    /**
     * 解析用户VIP等级
     * @param userId
     * @return
     */
    public LevelInfo analysisLevel(String userId){

        /** 根据用户当前消费 匹配对应的VIP等级 **/
        //获取用户消费金额
        BigDecimal consumption = financeDetailMapper.selectConsumptionByUser(userId);
        //获取所以VIP等级列表
        List<LevelInfo> levelInfoList = levelInfoMapper.selectAll();
        //目标VIP等级
        LevelInfo targetLevel = null;
        /** 根据用户消费计算目标等级 **/
        for(LevelInfo levelInfo : levelInfoList){
            if(consumption.compareTo(levelInfo.getRequirement()) >= 0){
                if(targetLevel == null){
                    targetLevel = levelInfo;
                }else if(targetLevel.getRequirement().compareTo(levelInfo.getRequirement()) < 0){
                    targetLevel = levelInfo;
                }
            }
        }

        /** 查询用户是不是推广人员 **/
        UserMedal userMedal = userMedalMapper.selectByUserId(userId);
        if(userMedal != null){
            /** 如果推广级别对应的用户等级  大于  消费计算的等级  则采用 推广级别对应的用户等级**/
            MedalInfo medalInfo = medalInfoMapper.selectByPrimaryKey(userMedal.getMedalId());
            LevelInfo medalLevel = levelInfoMapper.selectByPrimaryKey(medalInfo.getLevelId());
            if(targetLevel.getLevel() < medalLevel.getLevel()){
                targetLevel = medalLevel;
            }
        }

        /** 获取用户当前VIP等级 **/
        LevelInfo levelInfo = levelInfoMapper.selectByUser(userId);

        if(levelInfo == null || !targetLevel.getId().equals(levelInfo.getId())){
            UserLevel userLevel = userLevelMapper.selectByUser(userId);
            if(userLevel == null){
                userLevel = new UserLevel();
                userLevel.setLevelId(targetLevel.getId());
                userLevel.setUserId(userId);
                userLevel.setCreateTime(new Date());
                userLevel.setUpdateTime(new Date());
                userLevelMapper.insertSelective(userLevel);
            }else{
                userLevel.setLevelId(targetLevel.getId());
                userLevel.setUpdateTime(new Date());
                userLevelMapper.updateByPrimaryKeySelective(userLevel);
            }
        }
        return targetLevel;
    }


    /***
     * 校验代金券赠送
     * @param userId 注册用户ID
     * @param proUserId 推广用户ID
     */
    private void checkVoucherBind(String userId,String proUserId){

        /** 获取推广用户绑定的赠送代金券产品 **/
        List<VoucherBind> voucherBindList = voucherBindMapper.selectByUserId(proUserId);

        for(VoucherBind voucherBind : voucherBindList){

            VoucherProduct voucherProduct = voucherProductMapper.selectByPrimaryKey(voucherBind.getProductId());
            if(voucherProduct.getStatus() == 1){
                /** 代金券产品无效状态 **/
                continue;
            }

            /** 添加注册用户代金券信息 **/
            VoucherInfo voucherInfo = new VoucherInfo();
            voucherInfo.setUserId(userId);
            voucherInfo.setProductId(voucherProduct.getId());
            voucherInfo.setTotalAmount(voucherProduct.getTotalAmount());
            voucherInfo.setSurplusAmount(voucherProduct.getTotalAmount());
            voucherInfo.setStartTime(voucherProduct.getStartTime());
            voucherInfo.setEndTime(voucherProduct.getEndTime());
            voucherInfo.setStatus(0);
            voucherInfo.setCreateTime(new Date());
            voucherInfo.setUpdateTime(new Date());
            voucherInfoMapper.insertSelective(voucherInfo);

        }

    }

}
