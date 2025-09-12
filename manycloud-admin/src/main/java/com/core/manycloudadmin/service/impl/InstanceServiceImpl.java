package com.core.manycloudadmin.service.impl;

import com.core.manycloudadmin.service.InstanceService;
import com.core.manycloudadmin.so.instance.*;
import com.core.manycloudadmin.so.instance.RenewSO;
import com.core.manycloudcommon.caller.BaseCaller;
import com.core.manycloudcommon.caller.Item.ConfigItemVO;
import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.entity.TimerTask;
import com.core.manycloudcommon.enums.PowerStateEnum;
import com.core.manycloudcommon.enums.SyslogTypeEnum;
import com.core.manycloudcommon.enums.TaskTypeEnum;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.model.SyslogModel;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.DateUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.utils.StringUtils;
import com.core.manycloudcommon.vo.instance.InstanceDetailVO;
import com.core.manycloudcommon.vo.instance.InstanceUserVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InstanceServiceImpl implements InstanceService {

    @Autowired
    private InstanceInfoMapper instanceInfoMapper;

    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Autowired
    private PlatformInfoMapper platformInfoMapper;

    @Autowired
    private PlatformAccountMapper platformAccountMapper;

    @Autowired
    private TimerTaskMapper timerTaskMapper;

    @Autowired
    private NodeNetworkMapper nodeNetworkMapper;

    @Autowired
    private NodeModelMapper nodeModelMapper;

    @Autowired
    private NodeDiskMapper nodeDiskMapper;

    @Autowired
    private NodePriceMapper nodePriceMapper;

    @Autowired
    private LevelInfoMapper levelInfoMapper;

    @Autowired
    private UserFinanceMapper userFinanceMapper;

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    @Autowired
    private FinanceDetailMapper financeDetailMapper;

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private UserProMapper userProMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private MedalInfoMapper medalInfoMapper;

    @Autowired
    private UserMedalMapper userMedalMapper;

    @Autowired
    private UserLevelMapper userLevelMapper;

    @Autowired
    private MedalCommissionMapper medalCommissionMapper;

    @Autowired
    private CommissionDetailMapper commissionDetailMapper;

    @Autowired
    private SaleCommissionMapper saleCommissionMapper;

    @Autowired
    private SaleDetailMapper saleDetailMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    /***
     * 查询实例列表
     * @param queryListSO
     * @return
     */
    public ResultMessage queryList(QueryListSO queryListSO){
        PageHelper.startPage(queryListSO.getPage(), queryListSO.getPageSize());
        Page<InstanceUserVO> page = (Page<InstanceUserVO>)instanceInfoMapper.selectList(queryListSO.getAccount(),queryListSO.getInstanceId(),
                queryListSO.getPowerState(),queryListSO.getStatus(),queryListSO.getSort());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 查询实例详情信息
     * @param queryDetailSO
     * @return
     */
    public ResultMessage queryDetail(QueryDetailSO queryDetailSO){

        InstanceInfo instanceInfo = instanceInfoMapper.selectById(queryDetailSO.getInstanceId());
        String nodeName = nodeInfoMapper.selectNick(instanceInfo.getNodeId());
        NodeNetwork bandwidth = nodeNetworkMapper.selectByNode(instanceInfo.getNodeId(),0,instanceInfo.getModelId());
        NodeNetwork flow = nodeNetworkMapper.selectByNode(instanceInfo.getNodeId(),1,instanceInfo.getModelId());
        InstanceDetailVO detailVO = InstanceDetailVO.builder()
                .instanceId(instanceInfo.getInstanceId())
                .nodeName(nodeName)
                .cpu(instanceInfo.getCpu())
                .ram(instanceInfo.getRam())
                .account(instanceInfo.getConnectAccount())
                .pwd(instanceInfo.getConnectPwd())
                .image(instanceInfo.getImage())
                .bandwidth(instanceInfo.getBandwidth() == null  ? "" : CommonUtil.removeDecimal(instanceInfo.getBandwidth()))
                .bandwidthUnit(bandwidth == null || "N".equals(bandwidth.getExtendBl()) ? "" : bandwidth.getNetworkType())
                .flow(instanceInfo.getFlow() == null ? "" : CommonUtil.removeDecimal(instanceInfo.getFlow()))
                .flowUnit(flow == null || "N".equals(flow.getExtendBl()) ? "" : flow.getNetworkType())
                .sysDisk(instanceInfo.getSysDisk() == null ? "" : CommonUtil.removeDecimal(instanceInfo.getSysDisk()))
                .dataDisk(instanceInfo.getDataDisk() == null ? "" : CommonUtil.removeDecimal(instanceInfo.getDataDisk()))
                .status(instanceInfo.getStatus())
                .createTime(DateUtil.dateStr4(instanceInfo.getCreateTime()))
                .endTime(DateUtil.dateStr4(instanceInfo.getEndTime()))
                .build();

        Map<String,Object> result = new HashMap<>();
        result.put("instanceDetail",detailVO);


        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }


    /**
     * 主机电源操作
     * @param
     * @return
     */
    public ResultMessage execPower(ExecPowerSO execPowerSO){

        List<String> instanceIds= execPowerSO.getInstanceIds();
        int result = 0;
        for(String instanceId : instanceIds){

            try{

                boolean bl = false;
                InstanceInfo instanceInfo = instanceInfoMapper.selectById(instanceId);

                //获取默认资源平台账号
                PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(instanceInfo.getAccountId());
                /** 平台信息 **/
                PlatformInfo platformInfo = platformInfoMapper.selectByLabel(instanceInfo.getLabel());
                /** 节点可用区 **/
                NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());
                //项目ID
                String projectId = null;
                if(StringUtils.isNotEmpty(nodeInfo.getNodeParam())){
                    JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
                    projectId = param.get("projectId") == null ? null:param.getString("projectId");
                }
                AccountApi accountApi = AccountApi.builder()
                        .regionId(nodeInfo.getNodeVal())
                        .label(instanceInfo.getLabel())
                        .account(platformAccount.getAccount())
                        .keyNo(platformAccount.getKeyNo())
                        .keySecret(platformAccount.getKeySecret())
                        .baseUrl(platformAccount.getUrl())
                        .projectId(projectId)
                        .build();
                BaseCaller caller = BaseCaller.getCaller(accountApi);
                if("startup".equals(execPowerSO.getTag().toLowerCase())){//开机

                    StartSO startSO = StartSO.builder().instanceId(instanceInfo.getServiceNo()).build();
                    StartVO start = caller.start(startSO);
                    bl = CommonUtil.SUCCESS_CODE.equals(start.getCode());

                }else if("shutdown".equals(execPowerSO.getTag().toLowerCase())){//关机

                    StopSO stopSO = StopSO.builder().instanceId(instanceInfo.getServiceNo()).build();
                    StopVO stop = caller.stop(stopSO);
                    bl = CommonUtil.SUCCESS_CODE.equals(stop.getCode());

                }else if("restart".equals(execPowerSO.getTag().toLowerCase())){//重启

                    RebootSO rebootSO = RebootSO.builder().instanceId(instanceInfo.getServiceNo()).build();
                    RebootVO reboot = caller.reboot(rebootSO);
                    bl = CommonUtil.SUCCESS_CODE.equals(reboot.getCode());

                }

                if(bl){

                    /** 添加任务 **/
                    TimerTask timerTask = new TimerTask();
                    timerTask.setLabel(instanceInfo.getLabel());
                    timerTask.setType(TaskTypeEnum.EXECPOWER.getType());
                    timerTask.setTaskNo(instanceInfo.getInstanceId());
                    timerTask.setOrderNo(execPowerSO.getTag());
                    timerTask.setStatus(CommonUtil.STATUS_0);
                    timerTask.setRemark("电源操作:["+execPowerSO.getTag()+"]");
                    timerTask.setCreateTime(new Date());
                    timerTask.setUpdateTime(new Date());
                    timerTaskMapper.insertSelective(timerTask);

                    result++;

                    InstanceInfo entity = new InstanceInfo();
                    entity.setId(instanceInfo.getId());
                    entity.setPowerState(PowerStateEnum.EXECUTION.getVal());
                    entity.setUpdateTime(new Date());
                    instanceInfoMapper.updateByPrimaryKeySelective(entity);
                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }
        if(result >= instanceIds.size()){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"操作成功");
        }else if(result <= 0){
            return new ResultMessage(ResultMessage.FAILED_CODE,"操作失败");
        }else{
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"操作完成，成功 "+result+" 台。");
        }

    }

    /***
     * 查询续费价格
     * @param renewSO
     * @return
     */
    public BigDecimal queryRenewPrice(RenewSO renewSO){
        InstanceInfo instanceInfo = instanceInfoMapper.selectById(renewSO.getInstanceId());
        OrderSO orderSO = OrderSO.builder()
                .nodeId(instanceInfo.getNodeId())
                .modelId(instanceInfo.getModelId())
                .sysDisk(instanceInfo.getSysDisk())
                .dataDisk(instanceInfo.getDataDisk())
                .bandwidth(instanceInfo.getBandwidth())
                .flow(instanceInfo.getFlow())
                .num(1)
                .period(instanceInfo.getPeriod())
                .duration(renewSO.getDuration())
                .userId(instanceInfo.getUserId())
                .build();

        /** 获取续费价格 **/
        BigDecimal price = queryOrderPrice(orderSO);
        return price;
    }


    /***
     * 续费
     * @param renewSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage renew(RenewSO renewSO){
        InstanceInfo instanceInfo = instanceInfoMapper.selectById(renewSO.getInstanceId());

        /** 获取续费价格 **/
        BigDecimal price = queryRenewPrice(renewSO);

        if(price.compareTo(BigDecimal.valueOf(0)) < 1){
            return new ResultMessage(ResultMessage.FAILED_CODE,"无效金额，不支持续费。");
        }

        String userId = instanceInfo.getUserId();
        UserFinance userFinance = userFinanceMapper.selectByUserId(userId);
        if(price.compareTo(userFinance.getValidNum()) > 0){
            return new ResultMessage(ResultMessage.FAILED_CODE,"余额不足");
        }else{
            /** 冻结金额 **/
            int i = userFinanceMapper.updateBalanceByUserId(instanceInfo.getUserId(),"seal",price);
            if(i > 0){
                //用户余额更新记录
                UserFinance uf = userFinanceMapper.selectByUserId(userId);
                balanceLogMapper.insertChange(userId,"seal",price,uf.getValidNum(),"续费冻结金额");
            }
        }

        //获取默认资源平台账号
        PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(instanceInfo.getAccountId());
        /** 平台信息 **/
        PlatformInfo platformInfo = platformInfoMapper.selectByLabel(instanceInfo.getLabel());
        /** 节点可用区 **/
        NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());
        //项目ID
        String projectId = null;
        if(StringUtils.isNotEmpty(nodeInfo.getNodeParam())){
            JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
            projectId = param.get("projectId") == null ? null:param.getString("projectId");
        }

        AccountApi accountApi = AccountApi.builder()
                .regionId(nodeInfo.getNodeVal())
                .label(instanceInfo.getLabel())
                .account(platformAccount.getAccount())
                .keyNo(platformAccount.getKeyNo())
                .keySecret(platformAccount.getKeySecret())
                .baseUrl(platformAccount.getUrl())
                .projectId(projectId)
                .build();

        BaseCaller caller = BaseCaller.getCaller(accountApi);
        com.core.manycloudcommon.caller.so.RenewSO rs = com.core.manycloudcommon.caller.so.RenewSO.builder()
                .instanceId(instanceInfo.getServiceNo())
                .num(renewSO.getDuration())
                .build();
        try {
            RenewVO renewVO = caller.renew(rs);
            if(CommonUtil.SUCCESS_CODE.equals(renewVO.getCode())){
                /** 扣除金额 **/
                int i = userFinanceMapper.updateBalanceByUserId(instanceInfo.getUserId(),"minus",price);
                if(i > 0){
                    //用户余额更新记录
                    UserFinance uf = userFinanceMapper.selectByUserId(userId);
                    balanceLogMapper.insertChange(userId,"minus",price,uf.getValidNum(),"续费成功扣除金额");

                    //计算续费后的到期时间
                    OrderInfo orderInfo = orderInfoMapper.selectByNo(instanceInfo.getOrderNo());

                    //计算续费后的到期时间
                    Date newEndTime = null;
                    if(orderInfo.getPeriod() == 0){//按天购买
                        newEndTime = DateUtil.addDateDays(instanceInfo.getEndTime(),renewSO.getDuration());
                    }else if(orderInfo.getPeriod() == 1){//按月购买
                        newEndTime = DateUtil.daysBeMonth(instanceInfo.getEndTime(),renewSO.getDuration());

                    }else if(orderInfo.getPeriod() == 2){//按月购买
                        newEndTime = DateUtil.addDateDays(instanceInfo.getEndTime(),(renewSO.getDuration() * 30));
                    }

                    InstanceInfo entity = new InstanceInfo();
                    entity.setId(instanceInfo.getId());
                    entity.setEndTime(newEndTime);
                    entity.setStatus(3);//使用中状态
                    entity.setUpdateTime(new Date());
                    instanceInfoMapper.updateByPrimaryKeySelective(entity);

                    /** 添加财务账单明细 **/
                    FinanceDetail financeDetail = new FinanceDetail();
                    financeDetail.setUserId(instanceInfo.getUserId());
                    financeDetail.setFinanceNo(CommonUtil.getRandomNumber(12));
                    financeDetail.setProductNo(instanceInfo.getInstanceId());
                    financeDetail.setType(1);//消费类型
                    financeDetail.setMoneyNum(price);
                    financeDetail.setPeriod(renewSO.getDuration());
                    financeDetail.setTag("renew");//续费
                    financeDetail.setDirection(1);//支出
                    financeDetail.setWay(2);//交易方式(0:支付宝,1:微信,2:账号余额)
                    financeDetail.setStatus(1);//未完成
                    financeDetail.setCreateTime(new Date());
                    financeDetail.setUpdateTime(new Date());
                    financeDetailMapper.insertSelective(financeDetail);

                    String info = instanceInfo.getNike() == null ? instanceInfo.getPublicIp() : instanceInfo.getPublicIp()+"-"+ instanceInfo.getNike();
                    String endTimeStr = DateUtil.dateStr4(newEndTime);
                    String content = "尊敬的用户，您的实例["+info+"]已经续费成功，最新到期时间为 ["+endTimeStr+"]。";
                    SysLog sysLog = SyslogModel.output(instanceInfo.getUserId(), SyslogTypeEnum.RECHARGE,content);
                    sysLogMapper.insertSelective(sysLog);

                    /** 消费推广结算 **/
                    promotionCount(instanceInfo.getUserId(),instanceInfo.getInstanceId(),price,1);

                }
                return new ResultMessage(ResultMessage.SUCCEED_CODE,"续费成功");
            }else{
                /** 解冻金额 **/
                int i = userFinanceMapper.updateBalanceByUserId(instanceInfo.getUserId(),"unbind",price);
                if(i > 0){
                    //用户余额更新记录
                    UserFinance uf = userFinanceMapper.selectByUserId(userId);
                    balanceLogMapper.insertChange(userId,"unbind",price,uf.getValidNum(),"续费失败解冻金额");
                }
                return new ResultMessage(ResultMessage.SUCCEED_CODE,"续费失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            return new ResultMessage(ResultMessage.FAILED_CODE,"续费错误");
        }
    }

    /**
     * 查询购买价格
     * @param orderSO
     * @return
     */
    public BigDecimal queryOrderPrice(OrderSO orderSO){
        /** 配置数据 **/
        NodeModel model;
        if(orderSO.getModelId() == null){
            model = nodeModelMapper.selectByConfig(orderSO.getNodeId(),orderSO.getCpu(),orderSO.getRam());
        }else{
            model = nodeModelMapper.selectByPrimaryKey(orderSO.getModelId());
        }

        /** 磁盘数据 **/
        NodeDisk nodeDisk;
        if("Y".equals(model.getRegular())){
            nodeDisk = nodeDiskMapper.selectByNode(orderSO.getNodeId(),model.getId());
        }else{
            nodeDisk = nodeDiskMapper.selectByNode(orderSO.getNodeId(),null);
        }
        /** 带宽数据 **/
        NodeNetwork bandwidth;
        /** 流量数据 **/
        NodeNetwork flow;

        if("Y".equals(model.getRegular())){
            bandwidth = nodeNetworkMapper.selectByNode(orderSO.getNodeId(),0,model.getId());
            flow = nodeNetworkMapper.selectByNode(orderSO.getNodeId(),1,model.getId());
        }else{
            bandwidth = nodeNetworkMapper.selectByNode(orderSO.getNodeId(),0,null);
            flow = nodeNetworkMapper.selectByNode(orderSO.getNodeId(),1,null);
        }

        /** 购买价格 **/
        BigDecimal totalPrice = BigDecimal.valueOf(0);

        /** 计算基础配置价格 **/
        NodePrice modelPrice = nodePriceMapper.selectConfigPrice(orderSO.getNodeId(),"model",model.getId(),orderSO.getPeriod());
        if(modelPrice != null){
            totalPrice = totalPrice.add(modelPrice.getPrice());
        }


        /** 计算磁盘配置价格 **/
        NodePrice diskPrice = nodePriceMapper.selectConfigPrice(orderSO.getNodeId(),"disk",nodeDisk.getId(),orderSO.getPeriod());
        if(diskPrice != null){
            Integer diskNum = orderSO.getSysDisk().subtract(nodeDisk.getGiveNum()).intValue();
            if(orderSO.getDataDisk() != null){
                diskNum = diskNum + orderSO.getDataDisk().intValue();
            }
            Integer diskItem = diskPrice.getItem().intValue();
            BigDecimal singleDiskPrice = diskPrice.getPrice();

            BigDecimal p = diskNum == 0 || diskItem == 0? BigDecimal.valueOf(0) : BigDecimal.valueOf(diskNum).divide( BigDecimal.valueOf(diskItem),2,BigDecimal.ROUND_DOWN);
            totalPrice =  totalPrice.add(p.multiply(singleDiskPrice));
        }

        /** 计算带宽默认配置价格 **/
        NodePrice bandwidthPrice = nodePriceMapper.selectConfigPrice(orderSO.getNodeId(),"network",bandwidth.getId(),orderSO.getPeriod());
        if(bandwidthPrice != null){
            Integer bandwidthNum = orderSO.getBandwidth().intValue();
            Integer bandwidthItem = bandwidthPrice.getItem().intValue();
            BigDecimal singleBandwidthPrice = bandwidthPrice.getPrice();
            BigDecimal p = bandwidthNum == 0 || bandwidthItem == 0? BigDecimal.valueOf(0) : BigDecimal.valueOf(bandwidthNum).divide( BigDecimal.valueOf(bandwidthItem),2,BigDecimal.ROUND_DOWN);
            totalPrice =  totalPrice.add(p.multiply(singleBandwidthPrice));
        }


        /** 计算流量默认配置价格 **/
        NodePrice flowPrice = nodePriceMapper.selectConfigPrice(orderSO.getNodeId(),"network",flow.getId(),orderSO.getPeriod());
        if(flowPrice != null && orderSO.getFlow() != null){
            Integer flowNum = orderSO.getFlow().intValue();
            Integer flowItem = flowPrice.getItem().intValue();
            BigDecimal singleFlowPrice = flowPrice.getPrice();
            BigDecimal p = flowNum == 0 || flowItem == 0? BigDecimal.valueOf(0) : BigDecimal.valueOf(flowNum).divide( BigDecimal.valueOf(flowItem),2,BigDecimal.ROUND_DOWN);
            totalPrice =  totalPrice.add(p.multiply(singleFlowPrice));
        }

        /** 计算当前用户折扣 **/
        if(StringUtils.isNotEmpty(orderSO.getUserId())){
            /** 获取用户当前VIP等级 **/
            LevelInfo levelInfo = levelInfoMapper.selectByUser(orderSO.getUserId());
            if(levelInfo != null){
                BigDecimal discountPrice = totalPrice.multiply(levelInfo.getDiscount().divide(BigDecimal.valueOf(100))).setScale(2,BigDecimal.ROUND_UP);

                totalPrice = totalPrice.subtract(discountPrice);
            }
        }


        /** 单价 乘以 数量  乘以时长 **/
        totalPrice = totalPrice.multiply(BigDecimal.valueOf(orderSO.getNum())).multiply(BigDecimal.valueOf(orderSO.getDuration()));

        return totalPrice;
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
            LevelInfo levelInfo = analysisLevel(userId);

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
    @Override
    public ResultMessage getPlatformOverview(PlatformSo platformSo) {
        Map<String, Object> data = new LinkedHashMap<>();
        Integer total, expired, inUse, toBeRenewed;

        // 判断是否需要查询全部（空对象/空标签）
        boolean queryAll = platformSo == null
                || (platformSo.getPlatformLabel() == null
                || platformSo.getPlatformLabel().trim().isEmpty());

        // 1. 获取统计数据
        if (queryAll) {
            // 查询全部平台
            total = instanceInfoMapper.countTotalInstances();
            expired = instanceInfoMapper.countExpiredInstances();
            inUse = instanceInfoMapper.countInUseInstances();
            toBeRenewed = instanceInfoMapper.countToBeRenewedInstances();
        } else {
            // 按平台名称查询
            String platformName = platformSo.getPlatformLabel();
            total = instanceInfoMapper.countInstancesByPlatform(platformName);
            expired = instanceInfoMapper.countExpiredInstancesByPlatform(platformName);
            inUse = instanceInfoMapper.countInUseInstancesByPlatform(platformName);
            toBeRenewed = instanceInfoMapper.countToBeRenewedInstancesByPlatform(platformName);
        }

        data.put("total", total);
        data.put("expired", expired);
        data.put("inUse", inUse);
        data.put("toBeRenewed", toBeRenewed);

        // 2. 获取平台列表数据（同样应用queryAll判断）
        List<PlatformInfo> platforms;
        if (queryAll) {
            platforms = platformInfoMapper.selectAll();
        } else {
            String platformName = platformSo.getPlatformLabel();
            PlatformInfo platform = platformInfoMapper.selectByLabel(platformName);
            platforms = new ArrayList<>();
            if (platform != null) {
                platforms.add(platform);
            }
        }

        // 3. 封装平台统计详情
        List<Map<String, Object>> platformStats = new ArrayList<>();
        for (PlatformInfo platform : platforms) {
            Map<String, Object> platformStat = new HashMap<>();
            String label = platform.getLabel();
            platformStat.put("name", platform.getName());
            platformStat.put("total", instanceInfoMapper.countInstancesByPlatform(label));
            platformStat.put("expired", instanceInfoMapper.countExpiredInstancesByPlatform(label));
            platformStat.put("inUse", instanceInfoMapper.countInUseInstancesByPlatform(label));
            platformStat.put("toBeRenewed", instanceInfoMapper.countToBeRenewedInstancesByPlatform(label));
            platformStats.add(platformStat);
        }

        data.put("platformStats", platformStats);
        return new ResultMessage(ResultMessage.SUCCEED_CODE, ResultMessage.SUCCEED_MSG, data);

    }

    /**
     * 获取平台统计信息
     * @param platformSo
     * @return
     */
    @Override
    public ResultMessage getConfigDistribution(PlatformSo platformSo) {
        List<Map<String, Object>> configList = instanceInfoMapper.selectConfigDistribution(platformSo);

       // 封装响应数据
        ConfigDistributionVO vo = new ConfigDistributionVO();

        // 设置平台名称：仅判断对象是否为空，不处理"全部"文本
        if (platformSo == null) {
            // 空对象查询全部时，可根据实际需求设置平台名称，这里保持null或空字符串
            vo.setPlatform("全部"); // 或 ""，根据前端展示需求调整
        } else {
            // 非空对象直接使用其平台名称
            vo.setPlatform(platformSo.getPlatformLabel());
        }

        List<ConfigItemVO> items = new ArrayList<>();
        for (Map<String, Object> item : configList) {
            ConfigItemVO configItem = new ConfigItemVO();
            configItem.setName((String) item.get("name"));
            Number value = (Number) item.get("value");
            configItem.setValue(value.intValue());
            items.add(configItem);
        }
        vo.setItems(items);

        return new ResultMessage(ResultMessage.SUCCEED_CODE, ResultMessage.SUCCEED_MSG, vo);
    }

    @Override
    public ResultMessage selectPlatformList() {
        List<PlatformInfo> list = platformInfoMapper.selectAll();
        return new ResultMessage(ResultMessage.SUCCEED_CODE, ResultMessage.SUCCEED_MSG, list);
    }
}
