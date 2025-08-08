package com.core.manycloudtimer.job;


import com.core.manycloudcommon.caller.BaseCaller;
import com.core.manycloudcommon.caller.UcloudCaller;
import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.QueryDetailVO;
import com.core.manycloudcommon.caller.vo.QueryVO;
import com.core.manycloudcommon.caller.vo.UpdatePwdVO;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.entity.TimerTask;
import com.core.manycloudcommon.enums.PlatformLabelEnum;
import com.core.manycloudcommon.enums.PowerStateEnum;
import com.core.manycloudcommon.enums.SyslogTypeEnum;
import com.core.manycloudcommon.enums.TaskTypeEnum;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.model.SyslogModel;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.DateUtil;
import com.core.manycloudcommon.utils.HttpRequest;
import com.core.manycloudcommon.utils.StringUtils;
import com.core.manycloudtimer.util.WeiXinCaller;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 实例创建监控
 */
@Slf4j
@Component      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class CreateInstanceTimer {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private InstanceInfoMapper instanceInfoMapper;

    @Autowired
    private NodeAttributeMapper attributeMapper;

    @Autowired
    private NodePriceMapper nodePriceMapper;

    @Autowired
    private NodeNetworkMapper nodeNetworkMapper;

    @Autowired
    private NodeModelMapper nodeModelMapper;

    @Autowired
    private NodeDiskMapper nodeDiskMapper;

    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Autowired
    private NodeImageMapper nodeImageMapper;

    @Autowired
    private AttributeInfoMapper attributeInfoMapper;

    @Autowired
    private PlatformInfoMapper platformInfoMapper;

    @Autowired
    private PlatformAccountMapper platformAccountMapper;

    @Autowired
    private TimerTaskMapper timerTaskMapper;

    @Autowired
    private FinanceDetailMapper financeDetailMapper;

    @Autowired
    private UserFinanceMapper userFinanceMapper;

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    @Autowired
    private SysLogMapper sysLogMapper;

    @Value("${base_service.promotionCount}")
    private String promotionCountPath;

    @Autowired
    private WeiXinCaller weiXinCaller;


    /**
     * 实例创建
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void createInstanceListen(){
        List<TimerTask> timerTasks = timerTaskMapper.selectPendingByType(TaskTypeEnum.AHZ_BUY.getType());

        for(TimerTask timerTask : timerTasks){

            try{

                InstanceInfo instanceInfo = instanceInfoMapper.selectById(timerTask.getTaskNo());
                String serviceNo = timerTask.getOrderNo();

                /** 节点可用区 **/
                NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());

                //获取默认资源平台账号
                PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(instanceInfo.getAccountId());

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

                String shareId = null;
                NodeNetwork nodeNetwork = nodeNetworkMapper.selectByNode(nodeInfo.getId(),0,instanceInfo.getModelId());
                if(nodeNetwork != null){
                    if(StringUtils.isNotEmpty(nodeNetwork.getNetworkParam())){
                        JSONObject param = JSONObject.fromObject(nodeNetwork.getNetworkParam());
                        shareId = param.get("shareId") == null ? null:param.getString("shareId");
                    }
                }

                //获取客户端
                BaseCaller caller = BaseCaller.getCaller(accountApi);
                QuerySO querySO = QuerySO.builder()
                        .instanceIds(Arrays.asList(serviceNo))
                        .shareId(shareId)
                        .build();
                QueryVO queryVO = caller.createQuery(querySO);
                if(CommonUtil.SUCCESS_CODE.equals(queryVO.getCode())){

                    QueryDetailVO queryDetail = queryVO.getQueryDetailMap().get(serviceNo);

                    if(queryDetail == null){
                        log.info("[{}]平台创建实例[{}]查询异常----->{}",instanceInfo.getLabel(),serviceNo,queryVO.getMsg());
                        continue;
                    }

                    if(queryDetail.getStatus() == 1){//实例创建成功

                        PlatformLabelEnum labelEnum = PlatformLabelEnum.getByLabel(instanceInfo.getLabel());
                        if(labelEnum.isCteBl()){/** 创建延续任务 **/

                            TimerTask entity = new TimerTask();
                            entity.setLabel(instanceInfo.getLabel());
                            entity.setTaskNo(instanceInfo.getInstanceId());//实例ID
                            entity.setOrderNo(serviceNo);//主机编号
                            entity.setType(TaskTypeEnum.AHZ_BUY_CTE.getType());//异步创建主机延续
                            entity.setTag("N");//处理操作标记(Y/N)
                            entity.setStatus(0);//待处理
                            entity.setUpdateTime(new Date());
                            entity.setCreateTime(new Date());
                            timerTaskMapper.insertSelective(entity);

                        }else{
                            /** 实例创建完成 **/
                            complete(instanceInfo,serviceNo,queryDetail,nodeInfo.getNodeName());

                        }


                        timerTask.setRemark("实例创建完成");
                        timerTask.setStatus(2);//定时任务完成状态
                        timerTask.setUpdateTime(new Date());
                        timerTaskMapper.updateByPrimaryKeySelective(timerTask);

                    }else if(queryDetail.getStatus() == 2){//实例创建失败

                        instanceInfo.setStatus(7);//创建失败状态
                        instanceInfo.setUpdateTime(new Date());
                        instanceInfoMapper.updateByPrimaryKeySelective(instanceInfo);
                        /** 获取实例购买账单信息 **/
                        FinanceDetail financeDetail = financeDetailMapper.selectBuyByProduct(instanceInfo.getInstanceId());
                        if(financeDetail.getStatus() != 1){//账单未完成状态
                            //解冻用户余额
                            int i = userFinanceMapper.updateBalanceByUserId(financeDetail.getUserId(),"unbind",financeDetail.getMoneyNum());
                            if(i > 0){
                                //用户余额更新记录
                                UserFinance userFinance = userFinanceMapper.selectByUserId(financeDetail.getUserId());
                                balanceLogMapper.insertChange(financeDetail.getUserId(),"unbind",financeDetail.getMoneyNum(),userFinance.getValidNum(),"主机创建失败，解冻金额");

                                financeDetail.setStatus(2);//取消状态
                                financeDetail.setUpdateTime(new Date());
                                financeDetailMapper.updateByPrimaryKeySelective(financeDetail);
                            }
                        }

                        timerTask.setRemark("实例默认创建失败");
                        timerTask.setStatus(2);//定时任务完成状态
                        timerTask.setUpdateTime(new Date());
                        timerTaskMapper.updateByPrimaryKeySelective(timerTask);

                    }

                }else{
                    log.info("[{}]平台创建实例[{}]查询失败----->{}",instanceInfo.getLabel(),serviceNo,queryVO.getMsg());
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }



    /**
     * 实例创建延续任务
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void createInstanceContinue(){
        List<TimerTask> timerTasks = timerTaskMapper.selectPendingByType(TaskTypeEnum.AHZ_BUY_CTE.getType());

        for(TimerTask timerTask : timerTasks){

            try {

                InstanceInfo instanceInfo = instanceInfoMapper.selectById(timerTask.getTaskNo());
                String serviceNo = timerTask.getOrderNo();

                /** 节点可用区 **/
                NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());

                //获取默认资源平台账号
                PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(instanceInfo.getAccountId());

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
                //获取客户端
                BaseCaller caller = BaseCaller.getCaller(accountApi);

                //处理完成标记
                boolean bl = false;

                //查询实例信息
                QueryVO queryVO = caller.query(QuerySO.builder().instanceIds(Arrays.asList(serviceNo)).build());
                QueryDetailVO queryDetailVO = queryVO.getQueryDetailMap().get(serviceNo);

                if(PlatformLabelEnum.ALIYUN.getLabel().equals(instanceInfo.getLabel())){
                    /** 阿里云主机主机 创建完成后需要重新设置一下密码 **/

                    if("N".equals(timerTask.getTag())){//未操作

                        if(PowerStateEnum.RUNNING.getVal().equals(queryDetailVO.getPowerState())){
                            /** 运行中主机先关机 **/
                            caller.stop(StopSO.builder().instanceId(serviceNo).build());

                        }else if(PowerStateEnum.HALTED.getVal().equals(queryDetailVO.getPowerState())){

                            /** 已关机主机直接改密码 **/
                            UpdatePwdSO updatePwdSO = UpdatePwdSO.builder()
                                    .instanceId(serviceNo)
                                    .pwd(instanceInfo.getConnectPwd())
                                    .build();
                            UpdatePwdVO up = caller.updatePwd(updatePwdSO);
                            if(CommonUtil.SUCCESS_CODE.equals(up.getCode())){

                                caller.start(StartSO.builder().instanceId(serviceNo).build());
                                timerTask.setTag("Y");
                                timerTask.setUpdateTime(new Date());
                                timerTaskMapper.updateByPrimaryKeySelective(timerTask);
                            }else{
                                log.info("aliyun 初始化重置密码失败：{}",up.getMsg());
                            }
                        }
                    }else if("Y".equals(timerTask.getTag())){//已操作

                        if(PowerStateEnum.RUNNING.getVal().equals(queryDetailVO.getPowerState())){
                            bl = true;
                        }

                    }

                }else if(PlatformLabelEnum.AWSLS.getLabel().equals(instanceInfo.getLabel())){

                    if(PowerStateEnum.RUNNING.getVal().equals(queryDetailVO.getPowerState())){
                        /** AWS 主机 创建完成后需要重新设置一下密码 **/
                        UpdatePwdSO updatePwdSO = UpdatePwdSO.builder()
                                .instanceId(serviceNo)
                                .pwd(instanceInfo.getConnectPwd())
                                .build();
                        UpdatePwdVO up = caller.updatePwd(updatePwdSO);
                        if(CommonUtil.SUCCESS_CODE.equals(up.getCode())){
                            bl = true;
                        }else{
                            log.info("AWS LightSail 初始化重置密码失败：{}",up.getMsg());
                        }
                    }


                }


                if(bl){
                    /** 实例创建完成 **/
                    complete(instanceInfo,serviceNo,queryDetailVO,nodeInfo.getNodeName());

                    timerTask.setStatus(2);
                    timerTask.setUpdateTime(new Date());
                    timerTaskMapper.updateByPrimaryKeySelective(timerTask);
                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }


    /**
     * 实例创建完成
     * @param instanceInfo
     * @param serviceNo
     * @param queryDetail
     */
    private void complete(InstanceInfo instanceInfo,String serviceNo,QueryDetailVO queryDetail,String nodeName){
        instanceInfo.setConnectPort(queryDetail.getPort());
        instanceInfo.setConnectAccount(queryDetail.getAccount());
        instanceInfo.setServiceNo(serviceNo);
        if(StringUtils.isNotEmpty(queryDetail.getPwd())){
            instanceInfo.setConnectPwd(queryDetail.getPwd());
        }
        instanceInfo.setPublicIp(queryDetail.getPublicIp());
        instanceInfo.setPrivateIp(queryDetail.getPrivateIp());
        instanceInfo.setPowerState(PowerStateEnum.RUNNING.toString());
        instanceInfo.setStatus(3);//使用中状态
        instanceInfo.setUpdateTime(new Date());
        instanceInfoMapper.updateByPrimaryKeySelective(instanceInfo);
        /** 获取实例购买账单信息 **/
        FinanceDetail financeDetail = financeDetailMapper.selectBuyByProduct(instanceInfo.getInstanceId());
        if(financeDetail.getStatus() == 0){//账单未完成状态
            //扣除用户余额
            int i = userFinanceMapper.updateBalanceByUserId(financeDetail.getUserId(),"minus",financeDetail.getMoneyNum());
            if(i > 0){
                //用户余额更新记录
                UserFinance userFinance = userFinanceMapper.selectByUserId(financeDetail.getUserId());
                balanceLogMapper.insertChange(financeDetail.getUserId(),"minus",financeDetail.getMoneyNum(),userFinance.getValidNum(),"主机创建成功，扣除金额");

                financeDetail.setStatus(1);//完成状态
                financeDetail.setUpdateTime(new Date());
                financeDetailMapper.updateByPrimaryKeySelective(financeDetail);


                String info = instanceInfo.getNike() == null ? instanceInfo.getPublicIp() : instanceInfo.getPublicIp()+"-"+ instanceInfo.getNike();

                String content = "尊敬的用户，您的 ["+nodeName+"] 实例 ["+info+"] 已经购买成功。";
                SysLog sysLog = SyslogModel.output(instanceInfo.getUserId(), SyslogTypeEnum.RECHARGE,content);
                sysLogMapper.insertSelective(sysLog);

                /** 推广奖励-推送 **/
                Map<String,Object> promotionParam = new HashMap<>();
                promotionParam.put("userId",financeDetail.getUserId());
                promotionParam.put("productNo",instanceInfo.getInstanceId());
                promotionParam.put("amount",financeDetail.getMoneyNum().toPlainString());
                promotionParam.put("type",0);
                String paramStr = JSONObject.fromObject(promotionParam).toString();
                try{
                    HttpRequest.postJson(promotionCountPath,paramStr,null);
                }catch (Exception e){
                    log.info("推广奖励-参数：{}",paramStr);
                    log.info("推广奖励-异常：{}",e.getMessage());
                    e.printStackTrace();
                }

                /** 微信工作号购买成功通知 **/
                weiXinCaller.sendBuySuccess(financeDetail.getUserId(),nodeName+ "-" +info,financeDetail.getMoneyNum(),financeDetail.getUpdateTime());
            }
        }


        /** 订单完成确认 **/
        Integer confirm = instanceInfoMapper.selectOrderConfirm(instanceInfo.getOrderNo());
        if(confirm != null && confirm == 0){
            OrderInfo orderInfo = orderInfoMapper.selectByNo(instanceInfo.getOrderNo());
            orderInfo.setStatus(3);//完成状态
            orderInfo.setUpdateTime(new Date());
            orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
        }
    }






}
