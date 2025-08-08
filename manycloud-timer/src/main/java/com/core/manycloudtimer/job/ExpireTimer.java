package com.core.manycloudtimer.job;

import com.core.manycloudcommon.caller.BaseCaller;
import com.core.manycloudcommon.caller.so.DestroySO;
import com.core.manycloudcommon.caller.so.StopSO;
import com.core.manycloudcommon.caller.vo.DestroyVO;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.enums.PlatformLabelEnum;
import com.core.manycloudcommon.enums.PowerStateEnum;
import com.core.manycloudcommon.enums.SyslogTypeEnum;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.model.SyslogModel;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.DateUtil;
import com.core.manycloudcommon.utils.StringUtils;
import com.core.manycloudtimer.util.WeiXinCaller;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 实例到期定时器
 */
@Slf4j
@Component      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class ExpireTimer{

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private InstanceInfoMapper instanceInfoMapper;


    @Autowired
    private NodeInfoMapper nodeInfoMapper;


    @Autowired
    private PlatformAccountMapper platformAccountMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;


    @Autowired
    private WeiXinCaller weiXinCaller;




    /**
     * 实例待续费监控
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void pendingInstanceListen() {
        //获取使用中的实例列表
        List<InstanceInfo> list = instanceInfoMapper.selectByStatus(3);

        for (InstanceInfo instanceInfo : list) {
            try {

                Date endTime = instanceInfo.getEndTime();
                long time = endTime.getTime() - new Date().getTime();
                int num = (int)(time / (60 * 60 * 1000));

                if(num <= 72) {//距离过期时间小于等于3天 为待续费状态

                    InstanceInfo i = new InstanceInfo();
                    i.setId(instanceInfo.getId());
                    i.setStatus(4);//待续费状态
                    i.setUpdateTime(new Date());
                    instanceInfoMapper.updateByPrimaryKeySelective(i);

                    String info = instanceInfo.getNike() == null ? instanceInfo.getPublicIp() : instanceInfo.getPublicIp()+"-"+ instanceInfo.getNike();
                    String endTimeStr = DateUtil.dateStr4(endTime);
                    String content = "尊敬的用户，您的实例["+info+"]将于 ["+endTimeStr+"] 到期，如未续费主机将于到期后72小时自动销毁，请及时续费。";
                    SysLog sysLog = SyslogModel.output(instanceInfo.getUserId(), SyslogTypeEnum.NOTIFY,content);
                    sysLogMapper.insertSelective(sysLog);


                    //weiXinCaller.sendWaitRenew(instanceInfo.getUserId(),userInfo.getNickName(),userInfo.getAccount(),);

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 实例过期监控
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void expireInstanceListen() {
        //获取待续费的实例列表
        List<InstanceInfo> list = instanceInfoMapper.selectByStatus(4);

        for (InstanceInfo instanceInfo : list) {
            try {

                Date endTime = instanceInfo.getEndTime();

                if(endTime.getTime() < new Date().getTime()) {//距离过期时间小于等于3天 为待续费状态

                    InstanceInfo i = new InstanceInfo();
                    i.setPowerState(PowerStateEnum.HALTED.getVal());
                    i.setId(instanceInfo.getId());
                    i.setStatus(5);//过期状态
                    i.setUpdateTime(new Date());
                    instanceInfoMapper.updateByPrimaryKeySelective(i);

                    String info = instanceInfo.getNike() == null ? instanceInfo.getPublicIp() : instanceInfo.getPublicIp()+"-"+ instanceInfo.getNike();
                    String content = "尊敬的用户，您的实例["+info+"]已经到期，如未续费主机将于到期后72小时自动销毁，请及时续费。";
                    SysLog sysLog = SyslogModel.output(instanceInfo.getUserId(), SyslogTypeEnum.NOTIFY,content);
                    sysLogMapper.insertSelective(sysLog);


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
                    /** 关机 **/
                    caller.stop(StopSO.builder().instanceId(instanceInfo.getServiceNo()).build());

                    UserInfo userInfo = userInfoMapper.selectById(instanceInfo.getUserId());
                    weiXinCaller.sendExpire(instanceInfo.getUserId(),userInfo.getAccount(),0, "云主机 -" +info);

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    /**
     * 实例销毁监控
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void destroyInstanceListen() {
        //获取过期的实例列表
        List<InstanceInfo> list = instanceInfoMapper.selectByStatus(5);

        for (InstanceInfo instanceInfo : list) {
            try {

                Date endTime = instanceInfo.getEndTime();
                long time =  new Date().getTime() - endTime.getTime();
                int num = (int)(time / (60 * 60 * 1000));

                if(num >= 72) {//距离过期时间 大于 等于 3天 需要销毁主机

                    InstanceInfo i = new InstanceInfo();
                    i.setId(instanceInfo.getId());
                    i.setStatus(6);
                    i.setUpdateTime(new Date());
                    instanceInfoMapper.updateByPrimaryKeySelective(i);

                    PlatformLabelEnum platformLabelEnum = PlatformLabelEnum.getByLabel(instanceInfo.getLabel());
                    if(!platformLabelEnum.isDestroyBl()){

                        /** 节点可用区 **/
                        NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());

                        //获取资源平台账号
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
                        /** 销毁实例 **/
                        DestroyVO destroyVO = caller.destroy(DestroySO.builder().instanceId(instanceInfo.getServiceNo()).build());
                        if(CommonUtil.FAIL_CODE.equals(destroyVO.getCode())){
                            log.info("云主机[{} - {}] -> 到期销毁失败：{}",instanceInfo.getLabel(),instanceInfo.getInstanceId(),destroyVO.getMsg());
                        }

                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
