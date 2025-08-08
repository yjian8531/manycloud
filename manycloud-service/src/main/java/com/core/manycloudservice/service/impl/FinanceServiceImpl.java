package com.core.manycloudservice.service.impl;

import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.vo.finance.*;
import com.core.manycloudservice.service.FinanceService;
import com.core.manycloudservice.so.finance.*;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FinanceServiceImpl implements FinanceService {

    @Autowired
    private FinanceDetailMapper financeDetailMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserLevelMapper userLevelMapper;

    @Autowired
    private UserProMapper userProMapper;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private MedalInfoMapper medalInfoMapper;

    @Autowired
    private MedalCommissionMapper medalCommissionMapper;

    @Autowired
    private UserMedalMapper userMedalMapper;

    @Autowired
    private CommissionDetailMapper commissionDetailMapper;

    @Autowired
    private UserFinanceMapper userFinanceMapper;

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    @Autowired
    private SaleCommissionMapper saleCommissionMapper;

    @Autowired
    private SaleDetailMapper saleDetailMapper;

    @Autowired
    private LevelInfoMapper levelInfoMapper;

    @Autowired
    private InstanceInfoMapper instanceInfoMapper;

    @Autowired
    private FinanceWithdrawalMapper financeWithdrawalMapper;

    @Autowired
    private SysParamMapper sysParamMapper;

    @Autowired
    public Environment env;


    /**
     * 查询账单明细列表数据
     * @return
     */
    public ResultMessage queryList(QueryListSO queryListSO){
        PageHelper.startPage(queryListSO.getPage(), queryListSO.getPageSize());
        Page<FinanceDetailListVO> page = (Page<FinanceDetailListVO>)financeDetailMapper.selectList(queryListSO.getUserId(),queryListSO.getDirection(),queryListSO.getTag(),
                queryListSO.getStartTime(),queryListSO.getEndTime(),queryListSO.getProductNo(),queryListSO.getEmail());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 推广奖励
     * @param userId 消费用户ID
     * @param amount 消费金额
     * @param type 类型(0:购买,1:续费)
     */
    public void promotionCount(String userId, String productNo, BigDecimal amount,Integer type){

        new Thread(() -> {//异步执行返佣操作

            /** 计算用户最新等级 **/
            LevelInfo levelInfo = userService.analysisLevel(userId);

            /** 获取消费用户的推广人 **/
            UserPro userPro = userProMapper.selectByUserId(userId);
            if(userPro != null){
                //推广人ID
                String proUserId = userPro.getProUserId();
                UserInfo proUser = userInfoMapper.selectById(proUserId);

                if(proUser.getType() == 0){//普通用户

                    List<UserPro> proList = userProMapper.selectByProUserId(proUserId);
                    if(proList.size() > 0){
                        /** 查询推广人所有的推广消费总和 **/
                        List<String> userIds = proList.stream().map(up -> up.getUserId()).collect(Collectors.toList());
                        BigDecimal totalNum = financeDetailMapper.selectConsumptionByUsers(userIds);

                        List<MedalInfo> medalList = medalInfoMapper.selectAll();
                        //目标推广级别
                        MedalInfo targetMedal = null;
                        for(MedalInfo  medalInfo : medalList){
                            if(totalNum.compareTo(medalInfo.getPromotion()) >= 0){
                                if(targetMedal == null){
                                    targetMedal = medalInfo;
                                }else if(targetMedal.getPromotion().compareTo(medalInfo.getPromotion()) < 0){
                                    targetMedal = medalInfo;
                                }
                            }
                        }

                        if(targetMedal != null){
                            /** 更新推荐人的推广等级 **/
                            UserMedal userMedal = userMedalMapper.selectByUserId(proUserId);
                            if(userMedal == null){
                                userMedal = new UserMedal();
                                userMedal.setMedalId(targetMedal.getId());
                                userMedal.setUserId(proUserId);
                                userMedal.setCreateTime(new Date());
                                userMedal.setUpdateTime(new Date());
                                userMedalMapper.insertSelective(userMedal);
                            }else if(!targetMedal.getId().equals(userMedal.getMedalId())){
                                userMedal.setMedalId(targetMedal.getId());
                                userMedal.setUpdateTime(new Date());
                                userMedalMapper.updateByPrimaryKeySelective(userMedal);
                            }

                            /** 更新推荐人的用户等级 **/
                            LevelInfo proUserLevel = levelInfoMapper.selectByUser(proUserId);
                            if(proUserLevel == null){
                                UserLevel userLevel = new UserLevel();
                                userLevel.setUserId(proUserId);
                                userLevel.setLevelId(targetMedal.getLevelId());
                                userLevel.setCreateTime(new Date());
                                userLevel.setUpdateTime(new Date());
                                userLevelMapper.insertSelective(userLevel);
                            }else{
                                LevelInfo targetLevel = levelInfoMapper.selectByPrimaryKey(targetMedal.getLevelId());
                                if(proUserLevel.getLevel() < targetLevel.getLevel()){
                                    UserLevel userLevel = userLevelMapper.selectByUser(proUserId);
                                    userLevel.setLevelId(targetLevel.getId());
                                    userLevel.setUpdateTime(new Date());
                                    userLevelMapper.updateByPrimaryKeySelective(userLevel);
                                }

                            }

                            /** 根据推广人基本 和 用户等级获取推广分成比率 **/
                            MedalCommission medalCommission =  medalCommissionMapper.selectByRatio(targetMedal.getId(),levelInfo.getId());
                            if(medalCommission != null){

                                /** 结算渠道商的推广佣金 **/
                                BigDecimal commission = amount.multiply(medalCommission.getRatio().divide(BigDecimal.valueOf(100)));
                                //添加用户余额
                                int i = userFinanceMapper.updateBalanceByUserId(proUserId,"add",commission);
                                if(i > 0){
                                    //用户余额更新记录
                                    UserFinance userFinance = userFinanceMapper.selectByUserId(proUserId);
                                    balanceLogMapper.insertChange(proUserId,"add",commission,userFinance.getValidNum(),"推广返佣，添加金额");

                                    FinanceDetail financeDetail = new FinanceDetail();
                                    financeDetail.setUserId(proUserId);
                                    financeDetail.setFinanceNo(CommonUtil.getRandomStr(12));
                                    financeDetail.setType(0);//0:充值,1:消费,2:提现
                                    financeDetail.setMoneyNum(commission);
                                    financeDetail.setTag("commission");//佣金
                                    financeDetail.setDirection(0);//收入
                                    financeDetail.setWay(2);//交易方式(0:支付宝,1:微信,2:账号余额)
                                    financeDetail.setStatus(CommonUtil.STATUS_1);//完成状态
                                    financeDetail.setCreateTime(new Date());
                                    financeDetail.setUpdateTime(new Date());
                                    financeDetailMapper.insertSelective(financeDetail);
                                }

                                CommissionDetail commissionDetail = new CommissionDetail();
                                commissionDetail.setUserId(proUserId);
                                commissionDetail.setMedalId(targetMedal.getId());
                                commissionDetail.setLowUserId(userId);
                                commissionDetail.setLevelId(levelInfo.getId());
                                commissionDetail.setType(type);
                                commissionDetail.setProductNo(productNo);
                                commissionDetail.setConsumption(amount);
                                commissionDetail.setRatio(medalCommission.getRatio());
                                commissionDetail.setCommission(commission);
                                commissionDetail.setCreateTime(new Date());
                                commissionDetail.setUpdateTime(new Date());
                                commissionDetailMapper.insertSelective(commissionDetail);
                            }

                        }

                    }


                    /** 判断推荐人的上级是否为公司内部销售人员 **/
                    UserPro userTop = userProMapper.selectByUserId(proUserId);//查询推荐人的上级
                    if(userTop != null){
                        UserInfo topUser = userInfoMapper.selectById(userTop.getProUserId());
                        if(topUser.getType() != 0){//推荐人的上级是公司内部人员
                            /** 计算销售提成 **/
                            salesCommission(userId,topUser,type,productNo,amount,levelInfo);
                        }

                    }

                }else{//公司内部账号
                    /** 计算销售提成 **/
                    salesCommission(userId,proUser,type,productNo,amount,levelInfo);

                }
            }

        }).start();

    }


    /**
     * 计算销售提成
     * @param userId 消费用户ID
     * @param proUser 内部销售用户
     * @param type 类型(0:购买,1:续费)
     * @param productNo 产品ID
     * @param amount 消费金额
     * @param levelInfo 消费用户等级
     */
    private void salesCommission(String userId,UserInfo proUser,Integer type,String productNo,BigDecimal amount,LevelInfo levelInfo){
        /** 获取消费用户的推广等级 **/
        UserMedal userMedal = userMedalMapper.selectByUserId(userId);
        /** 角色(0:普通用户,1:推广渠道) **/
        Integer role = userMedal == null ? 0 : 1;
        /** 角色等级ID **/
        Integer roleLevel = role == 0 ? levelInfo.getId() : userMedal.getMedalId();

        SaleCommission saleCommission = saleCommissionMapper.selectRatio(proUser.getType(),role,roleLevel);
        if(saleCommission != null){

            /** 结算销售的提成 **/
            BigDecimal reward = amount.multiply(saleCommission.getRatio().divide(BigDecimal.valueOf(100)));
            SaleDetail saleDetail = new SaleDetail();
            saleDetail.setUserId(proUser.getUserId());
            saleDetail.setLowUserId(userId);
            saleDetail.setRole(role);
            saleDetail.setLevelId(roleLevel);
            saleDetail.setType(type);
            saleDetail.setProductNo(productNo);
            saleDetail.setConsumption(amount);
            saleDetail.setReward(reward);
            saleDetail.setRatio(saleCommission.getRatio());
            saleDetail.setCreateTime(new Date());
            saleDetail.setUpdateTime(new Date());
            saleDetailMapper.insertSelective(saleDetail);

        }
    }


    /***
     * 查询推广统计(客户端)
     * @param userId
     * @return
     */
    public ResultMessage queryCommissionStatistics(String userId){

        QueryCommissionStatisticsVO csv = commissionDetailMapper.selectStatisticsByUser(userId);

        List<UserPro> list = userProMapper.selectByProUserId(userId);
        //推广用户总数
        csv.setUserNum(list.size());

        if(list.size() > 0){
            //收集查询结果的所有用户ID
            List<String> userIds = list.stream().map(up -> up.getUserId()).collect(Collectors.toList());
            //查询推广用户的主机数量
            Integer productNum = instanceInfoMapper.selectNumByUsers(userIds);
            csv.setProductNum(productNum);
        }

        UserMedal userMedal = userMedalMapper.selectByUserId(userId);
        if(userMedal != null){
            MedalInfo medalInfo = medalInfoMapper.selectByPrimaryKey(userMedal.getMedalId());
            csv.setName(medalInfo.getName());
        }

        UserInfo userInfo = userInfoMapper.selectById(userId);
        String url = env.getProperty("register.market.url");
        url = url.replace("{0}",userInfo.getMarket());

        csv.setMarket(userInfo.getMarket());
        csv.setMarketUrl(url);

        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,csv);
    }


    /**
     * 查询推广明细列表(客户端)
     * @param queryUserProListSO
     * @return
     */
    public ResultMessage queryUserProList(QueryUserProListSO queryUserProListSO){

        PageHelper.startPage(queryUserProListSO.getPage(), queryUserProListSO.getPageSize());
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("userId",queryUserProListSO.getUserId());
        paramMap.put("account",queryUserProListSO.getAccount());
        paramMap.put("startTime",queryUserProListSO.getStartTime());
        paramMap.put("endTime",queryUserProListSO.getEndTime());
        Page<QueryUserProListVO> page = (Page<QueryUserProListVO>)userProMapper.selectList(paramMap);

        List<QueryUserProListVO> list = page.getResult();
        if(list.size() > 0) {
            //收集查询结果的所有用户ID
            List<String> userIds = list.stream().map(up -> up.getUserId()).collect(Collectors.toList());
            //查询用户产品路线数量
            List<UserProductNumVO> productNumList = instanceInfoMapper.selectListByUserSNum(userIds);
            Map<String,Integer> productNumMap = productNumList.stream().collect(Collectors.toMap(up -> up.getUserId(), up -> up.getNum()));
            for(QueryUserProListVO queryUserProListVO : list){
                queryUserProListVO.setProductNum(productNumMap.get(queryUserProListVO.getUserId()));
            }

        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",list);
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }

    /**
     * 查询返佣明细列表(客户端)
     * @param queryCommissionDetailListSO
     * @return
     */
    public ResultMessage queryCommissionDetailList(QueryCommissionDetailListSO queryCommissionDetailListSO){
        PageHelper.startPage(queryCommissionDetailListSO.getPage(), queryCommissionDetailListSO.getPageSize());
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("userId",queryCommissionDetailListSO.getUserId());
        paramMap.put("account",queryCommissionDetailListSO.getAccount());
        paramMap.put("startTime",queryCommissionDetailListSO.getStartTime());
        paramMap.put("endTime",queryCommissionDetailListSO.getEndTime());
        Page<QueryCommissionDetailListVO> page = (Page<QueryCommissionDetailListVO>)commissionDetailMapper.selectList(paramMap);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);

    }

    /***
     * 查询提现余额和税点
     * @param userId
     * @return
     */
    public ResultMessage getWithdrawalInfo(String userId){
        UserFinance userFinance = userFinanceMapper.selectByUserId(userId);
        SysParam sysParam = sysParamMapper.selectByTail("WITHDRAWAL_RATIO");
        Map<String,Object> result = new HashMap<>();
        result.put("balance",userFinance.getValidNum());
        result.put("ratio",new BigDecimal(sysParam.getVal()));
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }


    /***
     * 添加提现申请
     * @param addWithdrawalSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage addWithdrawal(AddWithdrawalSO addWithdrawalSO){

        UserFinance userFinance = userFinanceMapper.selectByUserId(addWithdrawalSO.getUserId());
        if(userFinance.getValidNum().compareTo(addWithdrawalSO.getMoneyNum()) < 0){
            return new ResultMessage(ResultMessage.FAILED_CODE,"余额不足");
        }

        if(addWithdrawalSO.getMoneyNum().compareTo(BigDecimal.valueOf(100)) < 0){
            return new ResultMessage(ResultMessage.FAILED_CODE,"提现金额不能低于 100 CNY");
        }

        FinanceWithdrawal financeWithdrawal = new FinanceWithdrawal();
        CommonUtil.copyProperties(addWithdrawalSO,financeWithdrawal);
        financeWithdrawal.setWithdrawalNo(CommonUtil.getRandomStr(8));
        financeWithdrawal.setStatus(0);
        financeWithdrawal.setCreateTime(new Date());
        financeWithdrawal.setUpdateTime(new Date());
        int i = financeWithdrawalMapper.insertSelective(financeWithdrawal);
        if(i > 0){

            /** 冻结提现金额 **/
            userFinanceMapper.updateBalanceByUserId(addWithdrawalSO.getUserId(),"seal",addWithdrawalSO.getMoneyNum());

            return new ResultMessage(ResultMessage.SUCCEED_CODE,"提交成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"提交失败");
        }

    }

    /**
     * 查询用户提现列表信息
     * @param queryWithdrawalSO
     * @return
     */
    public ResultMessage queryWithdrawal(QueryWithdrawalSO queryWithdrawalSO){
        PageHelper.startPage(queryWithdrawalSO.getPage(), queryWithdrawalSO.getPageSize());
        Page<FinanceWithdrawal> page = (Page<FinanceWithdrawal>)financeWithdrawalMapper.selectByUserId(queryWithdrawalSO.getUserId(),queryWithdrawalSO.getStatus());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


}
