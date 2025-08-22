package com.core.manycloudservice.service.impl;

import com.core.manycloudcommon.caller.BaseCaller;
import com.core.manycloudcommon.caller.UcloudCaller;
import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.entity.TimerTask;
import com.core.manycloudcommon.enums.PlatformLabelEnum;
import com.core.manycloudcommon.enums.PowerStateEnum;
import com.core.manycloudcommon.enums.TaskTypeEnum;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.DateUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.utils.StringUtils;
import com.core.manycloudcommon.vo.instance.InstanceDetailVO;
import com.core.manycloudcommon.vo.instance.InstanceUserVO;
import com.core.manycloudservice.service.InstanceService;
import com.core.manycloudservice.so.instance.*;
import com.core.manycloudservice.so.instance.UpdatePwdSO;
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

/**
 * 实例业务
 */
@Slf4j
@Service
public class InstanceServiceImpl implements InstanceService {

    @Autowired
    private InstanceInfoMapper instanceInfoMapper;

    @Autowired
    private GroupInfoMapper groupInfoMapper;

    @Autowired
    private GroupProductMapper groupProductMapper;

    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Autowired
    private FunctionInfoMapper functionInfoMapper;

    @Autowired
    private FunctionPlatformMapper functionPlatformMapper;

    @Autowired
    private PlatformInfoMapper platformInfoMapper;

    @Autowired
    private PlatformAccountMapper platformAccountMapper;

    @Autowired
    private TimerTaskMapper timerTaskMapper;

    @Autowired
    private NodeImageMapper nodeImageMapper;

    @Autowired
    private FinanceDetailMapper financeDetailMapper;

    @Autowired
    private NodeNetworkMapper nodeNetworkMapper;

    @Autowired
    private FirewallInfoMapper firewallInfoMapper;

    @Autowired
    private FirewallRuleMapper firewallRuleMapper;


    /***
     * 查询用户实例列表
     * @param queryListBuUserSO
     * @return
     */
    public ResultMessage queryListBuUser(String userId, QueryListBuUserSO queryListBuUserSO){
        PageHelper.startPage(queryListBuUserSO.getPage(), queryListBuUserSO.getPageSize());
        Page<InstanceUserVO> page = (Page<InstanceUserVO>)instanceInfoMapper.selectListByUser(userId,queryListBuUserSO.getInstanceId(),
                queryListBuUserSO.getPowerState(),queryListBuUserSO.getStatus(),queryListBuUserSO.getGroupId(),queryListBuUserSO.getSort());
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
        NodeImage nodeImage = nodeImageMapper.selectByPrimaryKey(instanceInfo.getImageId());
        NodeNetwork bandwidth = nodeNetworkMapper.selectByNode(instanceInfo.getNodeId(),0,instanceInfo.getModelId());
        NodeNetwork flow = nodeNetworkMapper.selectByNode(instanceInfo.getNodeId(),1,instanceInfo.getModelId());
        InstanceDetailVO detailVO = InstanceDetailVO.builder()
                .instanceId(instanceInfo.getInstanceId())
                .nodeName(nodeName)
                .publicIp(instanceInfo.getPublicIp())
                .privateIp(instanceInfo.getPrivateIp())
                .cpu(instanceInfo.getCpu())
                .ram(instanceInfo.getRam())
                .account(instanceInfo.getConnectAccount())
                .pwd(instanceInfo.getConnectPwd())
                .image(nodeImage != null ? "("+nodeImage.getImageType()+")"+nodeImage.getImageVersion() : instanceInfo.getImage())
                .bandwidth(instanceInfo.getBandwidth() == null ? "" : CommonUtil.removeDecimal(instanceInfo.getBandwidth()))
                .bandwidthUnit(bandwidth == null || "N".equals(bandwidth.getExtendBl()) ? "" : bandwidth.getNetworkType())
                .flow(instanceInfo.getFlow() == null ? "" : CommonUtil.removeDecimal(instanceInfo.getFlow()))
                .flowUnit(flow == null || "N".equals(flow.getExtendBl()) ? "" : flow.getNetworkType())
                .sysDisk(instanceInfo.getSysDisk() == null ? "" : CommonUtil.removeDecimal(instanceInfo.getSysDisk()))
                .dataDisk(instanceInfo.getDataDisk() == null ? "" : CommonUtil.removeDecimal(instanceInfo.getDataDisk()))
                .status(instanceInfo.getStatus())
                .createTime(DateUtil.dateStr4(instanceInfo.getCreateTime()))
                .endTime(DateUtil.dateStr4(instanceInfo.getEndTime()))
                .build();

        PlatformInfo platformInfo = platformInfoMapper.selectByLabel(instanceInfo.getLabel());

        List<FunctionInfo> list = functionInfoMapper.selectAll();
        List<FunctionPlatform> fnList = functionPlatformMapper.selectByPlatform(platformInfo.getId());
        Map<Integer,Object> fnMap = fnList.stream().collect(Collectors.toMap(f -> f.getFunctionId(),f -> f.getPlatformId()));

        List<Map<String,Object>> functionLsit =  new ArrayList<>();
        for(FunctionInfo functionInfo : list){
            Map<String,Object> functionMap = new HashMap<>();
            functionMap.put("functionId",functionInfo.getId());
            functionMap.put("functionName",functionInfo.getName());
            if(fnMap.get(functionInfo.getId()) != null){
                functionMap.put("tad","Y");
            }else{
                functionMap.put("tad","N");
            }
            functionLsit.add(functionMap);
        }

        Map<String,Object> result = new HashMap<>();
        result.put("instanceDetail",detailVO);
        result.put("functionLsit",functionLsit);

        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }


    /****
     * 修改别名
     * @param updateNikeSO
     * @return
     */
    public ResultMessage updateNike(UpdateNikeSO updateNikeSO){
        InstanceInfo instanceInfo = instanceInfoMapper.selectById(updateNikeSO.getInstanceId());
        if(instanceInfo != null){
            InstanceInfo entity = new InstanceInfo();
            entity.setId(instanceInfo.getId());
            entity.setNike(updateNikeSO.getNike());
            entity.setUpdateTime(new Date());
            int i = instanceInfoMapper.updateByPrimaryKeySelective(entity);
            if(i > 0){
                return new ResultMessage(ResultMessage.SUCCEED_CODE,"设置成功");
            }else{
                return new ResultMessage(ResultMessage.FAILED_CODE,"设置失败");
            }
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"无效的实例");
        }
    }


    /**
     * 添加产品分组信息
     * @param addGroupInfoSO
     * @return
     */
    public ResultMessage addGroupInfo(String userId, AddGroupInfoSO addGroupInfoSO){
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setUserId(userId);
        groupInfo.setName(addGroupInfoSO.getName());
        groupInfo.setRemark(addGroupInfoSO.getRemark());
        groupInfo.setNum(0);
        groupInfo.setUpdateTime(new Date());
        groupInfo.setCreateTime(new Date());
        int i = groupInfoMapper.insertSelective(groupInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"添加成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"添加失败");
        }
    }


    /**
     * 更新产品分组信息
     * @param updateGroupInfoSO
     * @return
     */
    public ResultMessage updateGroupInfo(UpdateGroupInfoSO updateGroupInfoSO){
        GroupInfo groupInfo = groupInfoMapper.selectByPrimaryKey(updateGroupInfoSO.getId());
        groupInfo.setName(updateGroupInfoSO.getName());
        groupInfo.setRemark(updateGroupInfoSO.getRemark());
        groupInfo.setUpdateTime(new Date());
        int i = groupInfoMapper.updateByPrimaryKeySelective(groupInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"更新成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"更新失败");
        }
    }

    /**
     * 删除产品分组信息
     * @param id 产品分组ID
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage deleteGroupInfo(Integer id){
        int i = groupInfoMapper.deleteByPrimaryKey(id);
        if(i > 0){
            groupProductMapper.deleteByGroupId(id);
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"删除成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"删除失败");
        }
    }


    /**
     * 查询当前用户所有的产品分组信息
     * @param userId
     * @return
     */
    public ResultMessage queryGroupByUserAll(String userId){
        Map<String,Object> param = new HashMap<>();
        param.put("userId",userId);
        List<GroupInfo> list = groupInfoMapper.selectList(param);
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }

    /**
     * 分页查询用户实例分组信息
     * @param userId
     * @return
     */
    public ResultMessage queryGroupByUserList(String userId, QueryGroupByUserListSO queryGroupByUserListSO){
        PageHelper.startPage(queryGroupByUserListSO.getPage(), queryGroupByUserListSO.getPageSize());
        Map<String,Object> param = new HashMap<>();
        param.put("userId",userId);
        param.put("name",queryGroupByUserListSO.getName());
        Page<GroupInfo> page = (Page<GroupInfo>)groupInfoMapper.selectList(param);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }

    /**
     * 添加实例分组关联
     * @param addGroupProductS0
     * @return
     */
    public ResultMessage addGroupProduct(AddGroupProductS0 addGroupProductS0){
        List<String> instanceIds = new ArrayList<>();
        for(String instanceId : addGroupProductS0.getInstanceIds()){
            GroupProduct groupProduct = groupProductMapper.selectByInstance(instanceId);
            if(groupProduct == null){
                instanceIds.add(instanceId);
            }
        }

        if(instanceIds.size() == 0){
            return new ResultMessage(ResultMessage.FAILED_CODE,"已存在分组里");
        }

        int i = groupProductMapper.insertBatch(addGroupProductS0.getGroupId(),instanceIds);
        if(i > 0){
            //更新分组数量
            groupInfoMapper.updateProductNum(addGroupProductS0.getGroupId());
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"添加成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"添加失败");
        }
    }

    /**
     * 移除实例分组关联
     * @param delGroupProductS0
     * @return
     */
    public ResultMessage delGroupProduct(String userId, DelGroupProductS0 delGroupProductS0){
        int i = groupProductMapper.deleteBatch(delGroupProductS0.getInstanceIds());
        if(i > 0){
            //更新分组数量
            Map<String,Object> param = new HashMap<>();
            param.put("userId",userId);
            List<GroupInfo> list = groupInfoMapper.selectList(param);
            for(GroupInfo groupInfo : list){
                groupInfoMapper.updateProductNum(groupInfo.getId());
            }
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"删除成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"删除失败");
        }
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




    /**
     * 更新实例密码
     * @param updatePwdSO
     * @return
     */
    public ResultMessage updatePwd(UpdatePwdSO updatePwdSO){

        InstanceInfo instanceInfo = instanceInfoMapper.selectById(updatePwdSO.getInstanceId());

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
        com.core.manycloudcommon.caller.so.UpdatePwdSO ups = com.core.manycloudcommon.caller.so.UpdatePwdSO.builder()
                .instanceId(instanceInfo.getServiceNo())
                .pwd(updatePwdSO.getPwd())
                .build();
        try{
            UpdatePwdVO updatePwdVO = caller.updatePwd(ups);
            if(CommonUtil.SUCCESS_CODE.equals(updatePwdVO.getCode())){
                InstanceInfo entity = new InstanceInfo();
                entity.setId(instanceInfo.getId());
                entity.setConnectPwd(updatePwdSO.getPwd());
                entity.setUpdateTime(new Date());
                instanceInfoMapper.updateByPrimaryKeySelective(entity);
                return new ResultMessage(ResultMessage.SUCCEED_CODE,"操作成功");
            }else{
                log.info("[{}]-实例[{}]更新密码失败：{}",instanceInfo.getLabel(),instanceInfo.getInstanceId(),updatePwdVO.getMsg());
                return new ResultMessage(ResultMessage.FAILED_CODE,"操作失败");
            }
        }catch (Exception e){
            log.info("[{}]-实例[{}]更新密码异常：{}",instanceInfo.getLabel(),instanceInfo.getInstanceId(),e.getMessage());
            e.printStackTrace();
            return new ResultMessage(ResultMessage.FAILED_CODE,"操作异常");
        }


    }


    /**
     * 重装查询镜像
     * @param resetSO
     * @return
     */
    public ResultMessage queryReset(ResetSO resetSO){
        InstanceInfo instanceInfo = instanceInfoMapper.selectById(resetSO.getInstanceId());
        List<NodeImage> list = nodeImageMapper.selectByNode(instanceInfo.getNodeId());
        Map<String,List<NodeImage>> result = new HashMap<>();
        for(NodeImage nodeImage : list){
            List<NodeImage> detailList = result.get(nodeImage.getImageType());
            if(detailList == null){
                detailList = new ArrayList<>();
            }
            detailList.add(nodeImage);
            result.put(nodeImage.getImageType(),detailList);
        }
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }


    /**
     * 重装系统
     * @return
     */
    public ResultMessage reset(ResetSO resetSO){

        InstanceInfo instanceInfo = instanceInfoMapper.selectById(resetSO.getInstanceId());
        if(!PowerStateEnum.HALTED.getVal().equals(instanceInfo.getPowerState())){
            return new ResultMessage(ResultMessage.FAILED_CODE,"请先进行关机");
        }

        //获取默认资源平台账号
        PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(instanceInfo.getAccountId());
        /** 平台信息 **/
        PlatformInfo platformInfo = platformInfoMapper.selectByLabel(instanceInfo.getLabel());

        /** 节点可用区 **/
        NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());
        ///项目ID
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
        ReinstallSO reinstallSO = ReinstallSO.builder()
                .instanceId(instanceInfo.getServiceNo())
                .imageId(resetSO.getOs())
                .pwd(instanceInfo.getConnectPwd())
                .build();

        try{
            ReinstallVO reinstallVO = caller.reinstall(reinstallSO);
            if(CommonUtil.SUCCESS_CODE.equals(reinstallVO.getCode())){

                /** 添加重启监控任务 **/
                TimerTask timerTask = new TimerTask();
                timerTask.setLabel(instanceInfo.getLabel());
                timerTask.setType(TaskTypeEnum.RESET.getType());
                timerTask.setTaskNo(instanceInfo.getInstanceId());
                timerTask.setOrderNo(instanceInfo.getServiceNo());
                timerTask.setStatus(CommonUtil.STATUS_0);
                timerTask.setRemark("N");
                timerTask.setCreateTime(new Date());
                timerTask.setUpdateTime(new Date());
                timerTaskMapper.insertSelective(timerTask);

                NodeImage nodeImage = nodeImageMapper.selectNodeParam(instanceInfo.getNodeId(),resetSO.getOs());
                InstanceInfo entity = new InstanceInfo();
                entity.setId(instanceInfo.getId());
                entity.setImageId(nodeImage.getId());
                entity.setImage(nodeImage.getImageVersion());
                entity.setPowerState(PowerStateEnum.EXECUTION.getVal());
                entity.setUpdateTime(new Date());
                instanceInfoMapper.updateByPrimaryKeySelective(entity);
                return new ResultMessage(ResultMessage.SUCCEED_CODE,"操作成功");
            }else{
                log.info("[{}]-实例[{}]重装系统失败：{}",instanceInfo.getLabel(),instanceInfo.getInstanceId(),reinstallVO.getMsg());
                return new ResultMessage(ResultMessage.FAILED_CODE,"操作失败");
            }
        }catch (Exception e){
            log.info("[{}]-实例[{}]重装系统异常：{}",instanceInfo.getLabel(),instanceInfo.getInstanceId(),e.getMessage());
            e.printStackTrace();
            return new ResultMessage(ResultMessage.FAILED_CODE,"操作异常");
        }

    }
    /**
     * 创建安全组
     * @param createSecuritySO
     * @return
     */

    @Override
    public ResultMessage createFirewall(CreateSecuritySO createSecuritySO) {
        try {
            //获取实例信息
            InstanceInfo instanceInfo = instanceInfoMapper.selectById(createSecuritySO.getInstanceId());

            if (!"UCLOUD".equals(instanceInfo.getLabel())) {
                return new ResultMessage(ResultMessage.FAILED_CODE, "当前仅支持UCLOUD平台安全组功能");
            }
            //获取默认资源平台账号
            PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(instanceInfo.getAccountId());
            NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());

            String projectId = null;
            if (StringUtils.isNotEmpty(nodeInfo.getNodeParam())) {
                JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
                projectId = param.get("projectId") == null ? null : param.getString("projectId");
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

            //创建安全组
            CreateSecurityVO createSecurityVO = caller.createFirewallTo(createSecuritySO);
            if (createSecurityVO == null || !CommonUtil.SUCCESS_CODE.equals(createSecurityVO.getCode())) {
                return new ResultMessage(ResultMessage.FAILED_CODE, "安全组创建失败");
            }

            //查询安全组
            QueryFirewallSO queryFirewallSO = QueryFirewallSO.builder()
                    .name(createSecuritySO.getName())
                    .fwId(createSecurityVO.getFwId())
                    .build();
            QueryFirewallVO queryFirewallVO = caller.queryFirewall(queryFirewallSO);
            if (queryFirewallVO == null || StringUtils.isEmpty(queryFirewallVO.getGroupId())) {
                return new ResultMessage(ResultMessage.FAILED_CODE, "查询安全组GroupId失败");
            }
            String groupId = queryFirewallVO.getGroupId();
            List<FirewallRule> rules = queryFirewallVO.getRules();

            //去重 逗号分隔
            Set<String> protocolSet = new LinkedHashSet<>();
            Set<String> portSet = new LinkedHashSet<>();

            // 遍历规则
            if (rules != null && !rules.isEmpty()) {
                for (FirewallRule rule : rules) {
                    if (StringUtils.isNotEmpty(rule.getProtocol())) {
                        protocolSet.add(rule.getProtocol());
                    }
                    // 保留原始格式，如 "22" 或 "1000-2000"
                    if (StringUtils.isNotEmpty(rule.getPort())) {
                        portSet.add(rule.getPort());
                    }
                }
            }

            // 拼接协议字符串（如 "TCP,UDP"）
            String protocolStr = String.join(",", protocolSet);
            // 拼接端口字符串（如 "22,1000-2000,8080"）
            String portStr = String.join(",", portSet);

            // 若查询无规则 则使用创建时的参数
            if (protocolStr.isEmpty()) {
                protocolStr = createSecuritySO.getProtocol();
            }
            if (portStr.isEmpty()) {
                portStr = createSecuritySO.getPort().toString();
            }

            FirewallInfo firewallInfo = new FirewallInfo();
            firewallInfo.setFirewallId(createSecurityVO.getFwId());// 安全组Id
            firewallInfo.setName(createSecuritySO.getName());// 安全组名称
            firewallInfo.setInstanceId(createSecuritySO.getInstanceId());// 实例Id
            firewallInfo.setUserId(instanceInfo.getUserId());// 用户Id
            firewallInfo.setPlatformLabel(instanceInfo.getLabel());// 平台标签
            firewallInfo.setStatus(1);// 状态
            firewallInfo.setCreateTime(new Date());
            firewallInfo.setUpdateTime(new Date());
            firewallInfo.setProtocol(protocolStr); // 协议
            firewallInfo.setPort(portStr);        // 端口

            firewallInfoMapper.insert(firewallInfo);

            // 绑定安全组
            GrantFirewallSO grantFirewallSO = GrantFirewallSO.builder()
                    .groupId(groupId)
                    .instanceId(instanceInfo.getServiceNo())
                    .build();
            GrantFirewallVO bindResult = caller.grantFirewall(grantFirewallSO);

            if (bindResult == null || !bindResult.isSuccess()) {
                Map<String, String> resultData = new HashMap<>();
                resultData.put("firewallId", createSecurityVO.getFwId());
                return new ResultMessage(ResultMessage.FAILED_CODE, "安全组创建成功但绑定失败", resultData);
            }

            Map<String, String> resultData = new HashMap<>();
            resultData.put("firewallId", createSecurityVO.getFwId());
            resultData.put("groupId", groupId);
            return new ResultMessage(ResultMessage.SUCCEED_CODE, "安全组创建并绑定成功", resultData);
        } catch (Exception e) {
            log.error("创建安全组异常", e);
            return new ResultMessage(ResultMessage.FAILED_CODE, "创建安全组异常：" + e.getMessage());
        }
    }

    public static void main(String[] args){

        Date endTime = DateUtil.fomatDate("2025-06-12 00:00:00");
        Date newTime = DateUtil.fomatDate("2025-03-12 00:30:20");

        /** 到期时间 减去 一个月 **/
        Date noTime = DateUtil.addDateMonths(endTime,-1);
        log.info("到期时间前一个月："+DateUtil.dateStr4(noTime));

        /** 到期时间 大于 一个月 **/
        if(noTime.getTime() > newTime.getTime()){

            String newMonthsStr = DateUtil.dateStrYYYYMM(newTime);
            String endMonthsStr = DateUtil.dateStrYYYYMM(endTime);
            int disparity = DateUtil.getMonthSub(newMonthsStr,endMonthsStr);

            Date disparityTime =  DateUtil.addDateMonths(endTime, BigDecimal.valueOf(disparity).negate().intValue());
            log.info("到期日期计算到当月："+DateUtil.dateStr4(disparityTime));
            if(disparityTime.getTime() < newTime.getTime()){
                disparity --;
            }

            System.out.println("应该退款月数:"+disparity);

        }

    }

    /***
     * 销毁退款查询
     * @param instanceDestroySO
     * @return
     */
    public ResultMessage queryDestroy(InstanceDestroySO instanceDestroySO){

        InstanceInfo instanceInfo = instanceInfoMapper.selectById(instanceDestroySO.getInstanceId());

        if(instanceInfo.getStatus() != 3){
            return new ResultMessage(ResultMessage.FAILED_CODE,"当前状态不支持销毁退款");
        }

        List<FinanceDetail> list = financeDetailMapper.selectProductByUser(instanceDestroySO.getUserId(),instanceDestroySO.getInstanceId());
        if(list.size() == 0){
            return new ResultMessage(ResultMessage.FAILED_CODE,"未发现当前产品的消费信息，请联系客服确认！");
        }

        /** 当前时间 **/
        Date newTime = new Date();
        /** 主机到期时间 **/
        Date endTime = instanceInfo.getEndTime();
        /** 到期时间 减去 一个月 **/
        Date noTime = DateUtil.addDateMonths(instanceInfo.getEndTime(),-1);

        /** 到期时间 大于 一个月 **/
        if(noTime.getTime() > newTime.getTime()){

            String newMonthsStr = DateUtil.dateStrYYYYMM(newTime);
            String endMonthsStr = DateUtil.dateStrYYYYMM(endTime);
            int disparity = DateUtil.getMonthSub(newMonthsStr,endMonthsStr);

            /** 到期日期计算到当月 **/
            Date disparityTime =  DateUtil.addDateMonths(endTime, BigDecimal.valueOf(disparity).negate().intValue());

            if(disparityTime.getTime() < newTime.getTime()){
                disparity --;
            }

            System.out.println("应该退款月数:"+disparity);

            /** 退款金额 **/
            BigDecimal amount = BigDecimal.valueOf(0);

            for (FinanceDetail financeDetail : list){
                /** 最新一次消费的作用时间 **/
                int m =  BigDecimal.valueOf(financeDetail.getPeriod()).negate().intValue();
                Date lisTime = DateUtil.addMonth(endTime,m);

                /** 退款开始时间 **/
                int d = BigDecimal.valueOf(disparity).negate().intValue();
                Date bisTime = DateUtil.addMonth(endTime,d);

                /** 开始退款时间 大于等于 最后一次消费作用时间 **/
                if(bisTime.getTime() >= lisTime.getTime()){

                    /** 一个月的单价 **/
                    BigDecimal price = financeDetail.getMoneyNum().divide(BigDecimal.valueOf(financeDetail.getPeriod()),2,BigDecimal.ROUND_UP);
                    amount = amount.add(price.multiply(BigDecimal.valueOf(disparity)));
                    break;
                }

            }

        }

        return null;
    }


}
