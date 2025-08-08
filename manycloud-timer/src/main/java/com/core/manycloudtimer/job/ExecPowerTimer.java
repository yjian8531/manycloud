package com.core.manycloudtimer.job;


import com.core.manycloudcommon.caller.BaseCaller;
import com.core.manycloudcommon.caller.so.QuerySO;
import com.core.manycloudcommon.caller.so.RebootSO;
import com.core.manycloudcommon.caller.so.UpdatePwdSO;
import com.core.manycloudcommon.caller.vo.QueryDetailVO;
import com.core.manycloudcommon.caller.vo.QueryVO;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.enums.PlatformLabelEnum;
import com.core.manycloudcommon.enums.TaskTypeEnum;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * 实例电源监控
 */
@Slf4j
@Component      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class ExecPowerTimer {

    @Autowired
    private InstanceInfoMapper instanceInfoMapper;

    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Autowired
    private TimerTaskMapper timerTaskMapper;

    @Autowired
    private PlatformInfoMapper platformInfoMapper;

    @Autowired
    private PlatformAccountMapper platformAccountMapper;

    /***
     * ucloud异步同步主机电源状态
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void dowerTask(){
        List<TimerTask> timerTasks =  timerTaskMapper.selectPendingByType(TaskTypeEnum.EXECPOWER.getType());
        for(TimerTask timerTask : timerTasks){

            try{
                InstanceInfo instanceInfo = instanceInfoMapper.selectById(timerTask.getTaskNo());
                String serviceNo = instanceInfo.getServiceNo();
                /** 节点可用区 **/
                NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());

                //获取默认资源平台账号
                PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(instanceInfo.getAccountId());
                /** 平台信息 **/
                PlatformInfo platformInfo = platformInfoMapper.selectByLabel(instanceInfo.getLabel());

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
                QueryVO queryVO = caller.query(QuerySO.builder().instanceIds(Arrays.asList(serviceNo)).build());
                if(CommonUtil.SUCCESS_CODE.equals(queryVO.getCode())){

                    QueryDetailVO queryDetail = queryVO.getQueryDetailMap().get(serviceNo);

                    if(queryDetail == null){
                        log.info("[{}]平台创建实例[{}]查询异常----->{}",instanceInfo.getLabel(),serviceNo,queryVO.getMsg());
                        continue;
                    }

                    if("restart".equals(timerTask.getOrderNo().toLowerCase()) || "startup".equals(timerTask.getOrderNo().toLowerCase())) {//开机。重启
                        if("running".toLowerCase().equals(queryDetail.getPowerState())){
                            InstanceInfo entity = new InstanceInfo();
                            entity.setId(instanceInfo.getId());
                            entity.setPowerState("running");
                            entity.setUpdateTime(new Date());
                            //更新电源状态
                            instanceInfoMapper.updateByPrimaryKeySelective(entity);

                            timerTask.setStatus(2);
                            timerTask.setUpdateTime(new Date());
                            timerTaskMapper.updateByPrimaryKeySelective(timerTask);
                        }
                    }else if("shutdown".equals(timerTask.getOrderNo().toLowerCase())){//关机
                        if("halted".toLowerCase().equals(queryDetail.getPowerState())){

                            InstanceInfo entity = new InstanceInfo();
                            entity.setId(instanceInfo.getId());
                            entity.setPowerState("halted");
                            entity.setUpdateTime(new Date());
                            //更新电源数据
                            instanceInfoMapper.updateByPrimaryKeySelective(entity);

                            timerTask.setStatus(2);
                            timerTask.setUpdateTime(new Date());
                            timerTaskMapper.updateByPrimaryKeySelective(timerTask);
                        }
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    /***
     * 重置密码监控
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void resetTask(){
        List<TimerTask> timerTasks =  timerTaskMapper.selectPendingByType(TaskTypeEnum.RESET.getType());
        for(TimerTask timerTask : timerTasks){

            try{
                InstanceInfo instanceInfo = instanceInfoMapper.selectById(timerTask.getTaskNo());
                String serviceNo = instanceInfo.getServiceNo();
                /** 节点可用区 **/
                NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());

                //获取默认资源平台账号
                PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(instanceInfo.getAccountId());
                /** 平台信息 **/
                PlatformInfo platformInfo = platformInfoMapper.selectByLabel(instanceInfo.getLabel());

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
                QueryVO queryVO = caller.query(QuerySO.builder().instanceIds(Arrays.asList(serviceNo)).build());
                if(CommonUtil.SUCCESS_CODE.equals(queryVO.getCode())){

                    QueryDetailVO queryDetail = queryVO.getQueryDetailMap().get(serviceNo);

                    if(queryDetail == null){
                        log.info("[{}]平台创建实例[{}]查询异常----->{}",instanceInfo.getLabel(),serviceNo,queryVO.getMsg());
                        continue;
                    }

                    if("running".toLowerCase().equals(queryDetail.getPowerState())){

                        /** 阿里云主机重装后密码会变，所以需要重置一下密码 **/
                        if(PlatformLabelEnum.ALIYUN.getLabel().equals(instanceInfo.getLabel())){
                            if("N".equals(timerTask.getRemark())){//没重置过密码
                                UpdatePwdSO updatePwdSO = UpdatePwdSO.builder()
                                        .pwd(instanceInfo.getConnectPwd())
                                        .instanceId(instanceInfo.getServiceNo())
                                        .build();
                                /** 重置密码 **/
                                caller.updatePwd(updatePwdSO);

                                /** 重启主机密码才会生效 **/
                                RebootSO rebootSO = RebootSO.builder().instanceId(instanceInfo.getServiceNo()).build();
                                caller.reboot(rebootSO);

                                timerTask.setRemark("Y");//标记已经重置过密码了
                                timerTask.setUpdateTime(new Date());
                                timerTaskMapper.updateByPrimaryKeySelective(timerTask);
                                continue;
                            }
                        }

                        InstanceInfo entity = new InstanceInfo();
                        entity.setId(instanceInfo.getId());
                        entity.setPowerState("running");
                        entity.setUpdateTime(new Date());
                        //更新电源状态
                        instanceInfoMapper.updateByPrimaryKeySelective(entity);

                        timerTask.setStatus(2);
                        timerTask.setUpdateTime(new Date());
                        timerTaskMapper.updateByPrimaryKeySelective(timerTask);
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


}
