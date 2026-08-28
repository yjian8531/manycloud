package com.core.manycloudcommon.caller;

import com.aliyun.teaopenapi.Client;
import com.aliyun.teaopenapi.models.OpenApiRequest;
import com.aliyun.teaopenapi.models.Params;
import com.aliyun.teautil.models.RuntimeOptions;
import com.core.manycloudcommon.caller.aliyun.FirewallTemplateRule;
import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.entity.ALiFirewallRule;
import com.core.manycloudcommon.enums.PowerStateEnum;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import java.util.*;

@Slf4j
public class AliyunCaller implements BaseCaller{

    private static Map<String, AliyunCaller> aliyunClientMap = new HashMap<>();

    private String regionId;

    private Client client;


    private AliyunCaller(String regionId, Client client){
        this.client = client;
        this.regionId = regionId;
    }

    /**
     * 获取客户端
     * @param accountApi
     * @return
     * @throws Exception
     */
    public static AliyunCaller getClient(AccountApi accountApi){

        if(aliyunClientMap.get(accountApi.getAccount()+":"+accountApi.getRegionId()) == null){
            synchronized(AliyunCaller.class){
                try {
                    com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                            // 必填，您的 AccessKey ID
                            .setAccessKeyId(accountApi.getKeyNo())
                            // 必填，您的 AccessKey Secret
                            .setAccessKeySecret(accountApi.getKeySecret());
                    // 访问的域名
                    config.endpoint = "swas."+accountApi.getRegionId()+".aliyuncs.com";
                    AliyunCaller aliyunCaller = new AliyunCaller(accountApi.getRegionId(),new Client(config));
                    aliyunClientMap.put(accountApi.getAccount()+":"+accountApi.getRegionId(),aliyunCaller);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            return aliyunClientMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }else{
            return aliyunClientMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }
    }


    private static Params createApiInfo(String acction) throws Exception {
        Params params = new Params()
                // 接口名称
                .setAction(acction)
                // 接口版本
                .setVersion("2020-06-01")
                // 接口协议
                .setProtocol("HTTPS")
                // 接口 HTTP 方法
                .setMethod("POST")
                .setAuthType("AK")
                .setStyle("RPC")
                // 接口 PATH
                .setPathname("/")
                // 接口请求体内容格式
                .setReqBodyType("json")
                // 接口响应体内容格式
                .setBodyType("json");
        return params;
    }

    /**
     * 执行请求
     * @param acction 查询接口
     * @param param 查询参数
     * @return
     * @throws Exception
     */
    private JSONObject exec(String acction,Map<String,Object> param) throws Exception{

        Params params = createApiInfo(acction);
        RuntimeOptions runtime = new RuntimeOptions();
        OpenApiRequest request;
        if(param == null || param.keySet().size() == 0){
            request = new OpenApiRequest();
        }else{
            request = new OpenApiRequest().setQuery(com.aliyun.openapiutil.Client.query(param));
        }
        runtime.readTimeout = 15000;
        Map result = client.callApi(params, request, runtime);

        return JSONObject.fromObject(result.get("body"));
    }

    /**
     * 执行请求（使用body参数，用于复杂对象数组）
     * @param action 接口名称
     * @param param 请求参数
     * @return
     * @throws Exception
     */
    private JSONObject execWithBody(String action, Map<String,Object> param) throws Exception{

        Params params = createApiInfo(action);
        RuntimeOptions runtime = new RuntimeOptions();
        OpenApiRequest request;

        if(param == null || param.keySet().size() == 0){
            request = new OpenApiRequest();
        }else{
            // 使用body传递参数（用于复杂的对象数组）
            request = new OpenApiRequest().setBody(com.aliyun.openapiutil.Client.parseToMap(param));
        }
        runtime.readTimeout = 15000;
        Map result = client.callApi(params, request, runtime);

        return JSONObject.fromObject(result.get("body"));
    }

    /**
     * 创建实例
     * @param createSO
     * @return
     */
    public CreateVO create(CreateSO createSO){
        String acction = "CreateInstances";//创建主机acction
        Map<String,Object> param = new HashMap<>();
        param.put("RegionId", regionId);//区域ID
        param.put("ImageId", createSO.getImageId());//镜像ID
        param.put("PlanId", createSO.getBundleId());//套餐ID
        param.put("Period", createSO.getPeriod());//周期月
        param.put("AutoRenew", false);//是否自动续费
        param.put("Amount", createSO.getNum());//数量
        param.put("ChargeType", "PrePaid");//包月模式
        param.put("ClientToken", UUID.randomUUID().toString());

        try{
            JSONObject result = exec(acction,param);
            log.info("阿里云主机创建结果:{}",result.toString());
            //主机服务ID
            JSONArray instanceIds = result.getJSONArray("InstanceIds");
            List<String> instanceIdList = new ArrayList<>();
            for(Object obj : instanceIds){
                instanceIdList.add(obj.toString());
            }
            return CreateVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .instanceIds(instanceIds)
                    .build();
        }catch (Exception e){
            e.printStackTrace();
            log.info("阿里云-创建轻量级云主机失败：{}",e.getMessage());
            return CreateVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }

    }


    /**
     * 查询创建实例
     * @param querySO
     * @return
     */
    public QueryVO createQuery(QuerySO querySO){

        String acction = "ListInstances";//查询实例信息

        /** 查询实例信息 **/
        Map<String,Object> param = new HashMap<>();
        param.put("InstanceIds",JSONArray.fromObject(querySO.getInstanceIds()).toString());
        try{
            JSONObject result  = exec(acction,param);

            JSONArray instanceList = result.getJSONArray("Instances");

            Map<String , QueryDetailVO> queryDetailMap = new HashMap<>();

            for(Object obj : instanceList){
                JSONObject instanceObj = JSONObject.fromObject(obj);

                String state = instanceObj.getString("Status").toLowerCase();
                String serviceNo = instanceObj.getString("InstanceId");
                String powerState;
                if("Starting".toLowerCase().equals(state) || "Upgrading".toLowerCase().equals(state)
                        || "Stopping".toLowerCase().equals(state) || "Resetting".toLowerCase().equals(state)){
                    /** 执行中 **/
                    powerState = PowerStateEnum.EXECUTION.getVal();
                }else if("Running".toLowerCase().equals(state.toLowerCase())){
                    /** 开机中 **/
                    powerState = PowerStateEnum.RUNNING.getVal();
                }else if("Stopped".toLowerCase().equals(state.toLowerCase())){
                    /** 已关机 **/
                    powerState = PowerStateEnum.HALTED.getVal();
                }else{
                    /** 未知状态标签 **/
                    powerState = state;
                }

                if("running".equals(state)){//正常运行状态

                    String osType = instanceObj.getJSONObject("Image").getString("OsType").toLowerCase();
                    String account = null;
                    Integer port = null;
                    if("windows".equals(osType)){
                        account = "administrator";
                        port = 3389;
                    }else if("ubuntu".equals(osType)){
                        account = "ubuntu";
                        port = 22;
                    }else if("linux".equals(osType) || "centos".equals(osType) || "debian".equals(osType) || "docky".equals(osType)){
                        account = "root";
                        port = 22;
                    }
                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(serviceNo)
                            .account(account)
                            .port(port)
                            .publicIp(instanceObj.getString("PublicIpAddress"))
                            .privateIp(instanceObj.getString("InnerIpAddress"))
                            .status(1)
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(serviceNo,odvo);
                }else if(state.indexOf("Disabled") > -1) {//主机创建失败

                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(serviceNo)
                            .status(2)
                            .msg("阿里云实例创建失败状态:"+state)
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(serviceNo,odvo);
                }else {
                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(serviceNo)
                            .status(0)
                            .msg("阿里云实例创建等待状态:"+state)
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(serviceNo,odvo);
                }

            }

            return QueryVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .queryDetailMap(queryDetailMap)
                    .build();

        }catch (Exception e){
            log.info("阿里云主机[{}]查询失败:{}",param.get("InstanceIds"),e.getMessage());
            e.printStackTrace();
            return QueryVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }


    /**
     * 查询创建实例
     * @param querySO
     * @return
     */
    public QueryVO query(QuerySO querySO){

        String acction = "ListInstances";//查询实例信息

        /** 查询实例信息 **/
        Map<String,Object> param = new HashMap<>();
        param.put("InstanceIds",JSONArray.fromObject(querySO.getInstanceIds()).toString());
        try{
            JSONObject result  = exec(acction,param);

            JSONArray instanceList = result.getJSONArray("Instances");

            Map<String , QueryDetailVO> queryDetailMap = new HashMap<>();

            for(Object obj : instanceList){
                JSONObject instanceObj = JSONObject.fromObject(obj);

                String state = instanceObj.getString("Status").toLowerCase();
                String serviceNo = instanceObj.getString("InstanceId");
                String powerState;
                if("Starting".toLowerCase().equals(state) || "Upgrading".toLowerCase().equals(state)
                        || "Stopping".toLowerCase().equals(state) || "Resetting".toLowerCase().equals(state)){
                    /** 执行中 **/
                    powerState = PowerStateEnum.EXECUTION.getVal();
                }else if("Running".toLowerCase().equals(state.toLowerCase())){
                    /** 开机中 **/
                    powerState = PowerStateEnum.RUNNING.getVal();
                }else if("Stopped".toLowerCase().equals(state.toLowerCase())){
                    /** 已关机 **/
                    powerState = PowerStateEnum.HALTED.getVal();
                }else{
                    /** 未知状态标签 **/
                    powerState = state;
                }

                String osType = instanceObj.getJSONObject("Image").getString("OsType").toLowerCase();
                String account = null;
                Integer port = null;
                if("windows".equals(osType)){
                    account = "administrator";
                    port = 3389;
                }else if("ubuntu".equals(osType)){
                    account = "ubuntu";
                    port = 22;
                }else if("linux".equals(osType) || "centos".equals(osType) || "debian".equals(osType) || "docky".equals(osType)){
                    account = "root";
                    port = 22;
                }
                QueryDetailVO odvo = QueryDetailVO.builder()
                        .serviceNo(serviceNo)
                        .account(account)
                        .port(port)
                        .publicIp(instanceObj.getString("PublicIpAddress"))
                        .privateIp(instanceObj.getString("InnerIpAddress"))
                        .status(1)
                        .powerState(powerState)
                        .build();
                queryDetailMap.put(serviceNo,odvo);

            }

            return QueryVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .queryDetailMap(queryDetailMap)
                    .build();

        }catch (Exception e){
            log.info("阿里云主机[{}]查询失败:{}",param.get("InstanceIds"),e.getMessage());
            e.printStackTrace();
            return QueryVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 实例续费
     * @param renewSO
     * @return
     */
    public RenewVO renew(RenewSO renewSO){
        String acction = "RenewInstance";//主机续费

        Map<String,Object> param = new HashMap<>();
        param.put("RegionId", regionId);//区域ID

        param.put("InstanceId", renewSO.getInstanceId());//主机ID
        param.put("Period", renewSO.getNum());//周期月
        //param.put("ClientToken", UUID.randomUUID().toString());

        try{
            JSONObject result = exec(acction,param);
            log.info("阿里云主机[{}]续费成功:{}",renewSO.getInstanceId(),result.toString());
            return RenewVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            e.printStackTrace();
            log.info("阿里云-轻量级云主机[{}]实例续费失败：{}",renewSO.getInstanceId(),e.getMessage());
            return RenewVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }

    }

    /**
     * 开机
     * @param startSO
     * @return
     * @throws Exception
     */
    public StartVO start(StartSO startSO){
        String acction = "StartInstance";//批量开机

        Map<String,Object> param = new HashMap<>();
        param.put("RegionId", regionId);//区域ID
        param.put("InstanceId", startSO.getInstanceId());//主机ID集合
        param.put("ClientToken", UUID.randomUUID().toString());

        try{
            JSONObject result = exec(acction,param);
            log.info("阿里云主机-开机成功:{}",result.toString());
            return StartVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            e.printStackTrace();
            log.info("阿里云主机[{}]-开机失败：{}",startSO.getInstanceId(),e.getMessage());
            return StartVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }


    /**
     * 重启
     * @param rebootSO
     * @return
     * @throws Exception
     */
    public RebootVO reboot(RebootSO rebootSO){
        String acction = "RebootInstance";//批量重启

        Map<String,Object> param = new HashMap<>();
        param.put("RegionId", regionId);//区域ID
        param.put("InstanceId", rebootSO.getInstanceId());//主机ID集合
        param.put("ClientToken", UUID.randomUUID().toString());

        try{
            JSONObject result = exec(acction,param);
            log.info("阿里云主机-重启成功:{}",result.toString());
            return RebootVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            e.printStackTrace();
            log.info("阿里云主机-[{}]-重启失败：{}",rebootSO.getInstanceId(),e.getMessage());
            return RebootVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }


    /**
     * 关机
     * @param stopSO
     * @return
     * @throws Exception
     */
    public StopVO stop(StopSO stopSO){
        String acction = "StopInstance";//批量关机

        Map<String,Object> param = new HashMap<>();
        param.put("RegionId", regionId);//区域ID
        param.put("InstanceId", stopSO.getInstanceId());//主机ID集合
        param.put("ClientToken", UUID.randomUUID().toString());

        try{
            JSONObject result = exec(acction,param);
            log.info("阿里云主机-关机成功:{}",result.toString());
            return StopVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            e.printStackTrace();
            log.info("阿里云-轻量级云主机{} 关机失败：{}",stopSO.getInstanceId(),e.getMessage());
            return StopVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }

    }


    /**
     * 重装系统
     * @param reinstallSO
     * @return
     * @throws Exception
     */
    public ReinstallVO reinstall(ReinstallSO reinstallSO){

        String acction = "ResetSystem";//重装系统

        Map<String,Object> param = new HashMap<>();
        param.put("RegionId", regionId);//区域ID
        param.put("InstanceId",reinstallSO.getInstanceId());//主机ID
        param.put("ImageId",reinstallSO.getImageId());//镜像ID
        param.put("ClientToken", UUID.randomUUID().toString());

        try{
            JSONObject result = exec(acction,param);
            log.info("阿里云主机-重装成功:{}",result.toString());
            return ReinstallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            e.printStackTrace();
            log.info("阿里云-轻量级云主机{} 重装失败：{}",reinstallSO.getInstanceId(),e.getMessage());
            return ReinstallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }

    }

    /**
     * 销毁
     * @param destroySO
     * @return
     */
    public DestroyVO destroy(DestroySO destroySO){
        return DestroyVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿里云-不支持此功能")
                .build();
    }


    /**
     * 设置主机自动续费标识
     * @param updateAuteRenewSO
     * @return
     */
    public UpdateAuteRenewVO updateAuteRenew(UpdateAuteRenewSO updateAuteRenewSO){
        return UpdateAuteRenewVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿里云-不支持此功能")
                .build();
    }

    /**
     * 更新主机密码
     * @param updatePwdSO
     * @return
     * @throws Exception
     */
    public UpdatePwdVO updatePwd(UpdatePwdSO updatePwdSO){
        String acction = "UpdateInstanceAttribute";//修改密码

        Map<String,Object> param = new HashMap<>();
        param.put("InstanceId",updatePwdSO.getInstanceId());
        param.put("Password",updatePwdSO.getPwd());
        param.put("ClientToken", UUID.randomUUID().toString());

        try{
            JSONObject result = exec(acction,param);
            log.info("阿里云主机-改密成功:{}",result.toString());
            return UpdatePwdVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            e.printStackTrace();
            log.info("阿里云-轻量级云主机{} 改密失败：{}",updatePwdSO.getInstanceId(),e.getMessage());
            return UpdatePwdVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 创建防火墙规则（轻量应用服务器SWAS）
     **/
    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO) throws Exception {
        // SWAS使用CreateFirewallRules API直接创建防火墙规则
        String action = "CreateFirewallRules";
        Map<String, Object> param = new HashMap<>();

        // 必填参数
        param.put("RegionId", regionId);
        param.put("InstanceId", createSecuritySO.getInstanceId());

        // 防火墙规则数组
        List<Map<String, Object>> rules = new ArrayList<>();

        // 处理通用格式rules
        if (createSecuritySO.getRules() != null && !createSecuritySO.getRules().isEmpty()) {
            for (CreateSecuritySO.FirewallRule rule : createSecuritySO.getRules()) {
                Map<String, Object> ruleMap = new HashMap<>();
                // 自动转大写，兼容用户输入的小写协议
                String protocol = rule.getProtocol() != null ? rule.getProtocol().toUpperCase() : "TCP";
                ruleMap.put("RuleProtocol", protocol);
                ruleMap.put("Port", rule.getPort());
                ruleMap.put("SourceCidrIp", rule.getSource());
                if (rule.getDescription() != null) {
                    ruleMap.put("Remark", rule.getDescription());
                }
                rules.add(ruleMap);
            }
        }
        // 处理阿里云格式firewallRules
        else if (createSecuritySO.getFirewallRules() != null && !createSecuritySO.getFirewallRules().isEmpty()) {
            for (ALiFirewallRule firewallRule : createSecuritySO.getFirewallRules()) {
                Map<String, Object> ruleMap = new HashMap<>();
                // 自动转大写，兼容用户输入的小写协议
                String ruleProtocol = firewallRule.getRuleProtocol() != null ? firewallRule.getRuleProtocol().toUpperCase() : "TCP";
                ruleMap.put("RuleProtocol", ruleProtocol);
                ruleMap.put("Port", firewallRule.getPort());
                ruleMap.put("SourceCidrIp", firewallRule.getSourceCidrIp());
                if (firewallRule.getRemark() != null) {
                    ruleMap.put("Remark", firewallRule.getRemark());
                }
                rules.add(ruleMap);
            }
        }

        if (!rules.isEmpty()) {
            param.put("FirewallRules", rules);
        }

        try {
            JSONObject result = execWithBody(action, param);  // 使用execWithBody方法
            log.info("创建防火墙规则成功: {}", result.toString());

            return CreateSecurityVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .fwId(createSecuritySO.getInstanceId()) // 返回实例ID作为防火墙标识
                    .build();

        } catch (Exception e) {
            log.error("创建防火墙规则失败: ", e);
            return CreateSecurityVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg("创建防火墙规则失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 查询防火墙规则（轻量应用服务器SWAS）
     **/
    @Override
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception {
        // SWAS使用ListFirewallRules API查询防火墙规则
        String action = "ListFirewallRules";
        Map<String, Object> param = new HashMap<>();

        // 必填参数
        param.put("RegionId", regionId);
        // 优先使用fwId（真实的云平台实例ID），如果没有则使用instanceId
        String realInstanceId = StringUtils.isNotEmpty(queryFirewallSO.getFwId())
                ? queryFirewallSO.getFwId()
                : queryFirewallSO.getInstanceId();
        param.put("InstanceId", realInstanceId);

        try {
            JSONObject result = exec(action, param);
//            log.info("查询防火墙规则成功: {}", result.toString());

            // 解析返回数据
            QueryFirewallVO vo = QueryFirewallVO.builder()
                    .requestId(result.getString("RequestId"))
                    .build();

            // 解析 FirewallRules 数组
            JSONArray firewallRulesJson = result.getJSONArray("FirewallRules");
            List<com.core.manycloudcommon.entity.FirewallRule> rules = new ArrayList<>();

            if (firewallRulesJson != null) {
                for (Object obj : firewallRulesJson) {
                    JSONObject ruleObj = JSONObject.fromObject(obj);

                    com.core.manycloudcommon.entity.FirewallRule rule = new com.core.manycloudcommon.entity.FirewallRule();
                    rule.setProtocol(ruleObj.getString("RuleProtocol"));
                    rule.setPort(ruleObj.getString("Port"));
                    rule.setIpAddress(ruleObj.getString("SourceCidrIp"));
                    rule.setRemark(ruleObj.getString("Remark"));  // Remark对应description
                    rule.setAction(ruleObj.getString("Policy"));   // Policy对应action
                    rule.setFirewallId(queryFirewallSO.getInstanceId()); // 使用实例ID作为防火墙ID
                    rules.add(rule);
                }
            }

            // 设置返回值
            vo.setFwId(queryFirewallSO.getInstanceId()); // 使用实例ID作为fwId
            vo.setGroupId(queryFirewallSO.getInstanceId()); // 使用实例ID作为groupId
            vo.setName(queryFirewallSO.getInstanceId()); // 使用实例ID作为name
            vo.setRules(rules);
            vo.setCode(CommonUtil.SUCCESS_CODE);
            vo.setMsg(CommonUtil.SUCCESS_MSG);

            return vo;

        } catch (Exception e) {
            log.error("查询防火墙规则失败: ", e);
            return QueryFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg("查询防火墙规则失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public CreateFirewallTemplateRulesVO createFirewallTemplateRules(CreateFirewallTemplateRulesSO so) {
        String acction = "CreateFirewallTemplateRules";

        Map<String, Object> param = new HashMap<>();

        // 必填参数
        param.put("RegionId", regionId);
        param.put("FirewallTemplateId", so.getFirewallTemplateId());

        // 防火墙规则数组
        if (so.getFirewallRules() == null || so.getFirewallRules().isEmpty()) {
            throw new IllegalArgumentException("防火墙规则不能为空");
        }

        List<Map<String, Object>> rules = new ArrayList<>();
        for (ALiFirewallRule rule : so.getFirewallRules()) {
            Map<String, Object> ruleMap = new HashMap<>();
            ruleMap.put("RuleProtocol", rule.getRuleProtocol());
            ruleMap.put("Port", rule.getPort());
            ruleMap.put("SourceCidrIp", rule.getSourceCidrIp());
            if (rule.getRemark() != null) {
                ruleMap.put("Remark", rule.getRemark());
            }
            rules.add(ruleMap);
        }
        param.put("FirewallRule", rules);

        try {
            JSONObject result = exec(acction, param);
            log.info("创建防火墙模板规则成功: {}", result.toString());

            // 解析返回结果
            String requestId = result.getString("RequestId");

            // 解析 FirewallTemplateRules 数组
            JSONArray rulesJson = result.getJSONArray("FirewallTemplateRules");
            List<FirewallTemplateRule> ruleList = new ArrayList<>();

            for (Object obj : rulesJson) {
                JSONObject ruleObj = JSONObject.fromObject(obj);
                FirewallTemplateRule rule = new FirewallTemplateRule();
                rule.setFirewallTemplateRuleId(ruleObj.getString("FirewallTemplateRuleId"));
                rule.setRuleProtocol(ruleObj.getString("RuleProtocol"));
                rule.setPort(ruleObj.getString("Port"));
                rule.setSourceCidrIp(ruleObj.getString("SourceCidrIp"));
                rule.setRemark(ruleObj.getString("Remark"));
                ruleList.add(rule);
            }

            return CreateFirewallTemplateRulesVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .requestId(requestId)
                    .firewallTemplateRules(ruleList)
                    .build();

        } catch (Exception e) {
            log.error("创建防火墙模板规则失败: ", e);
            return CreateFirewallTemplateRulesVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 授予安全组权限
     **/
    @Override
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO) throws Exception {
        String acction = "ApplyFirewallTemplate"; // 接口名称

        Map<String, Object> param = new HashMap<>();

        // 必填参数
        param.put("RegionId", regionId);
        param.put("FirewallTemplateId", grantFirewallSO.getFirewallTemplateId());

        // 实例 ID 列表（数组）
//        List<String> instanceIds = grantFirewallSO.getInstanceIds();
        String instanceIds = grantFirewallSO.getInstanceId();
        if (instanceIds == null || instanceIds.isEmpty()) {
            throw new IllegalArgumentException("实例ID列表不能为空");
        }
//        // 确保不超过10个
//        if (instanceIds.size() > 10) {
//            throw new IllegalArgumentException("实例ID数量不能超过10个");
//        }
        param.put("InstanceIds", JSONArray.fromObject(instanceIds).toString()); // 转为 JSON 字符串

        // 可选参数：ClientToken，用于幂等性
        param.put("ClientToken", UUID.randomUUID().toString());

        try {
            JSONObject result = exec(acction, param);
            log.info("应用防火墙模板成功: {}", result.toString());

            // 解析返回结果
            String requestId = result.getString("RequestId");
            String taskId = result.getString("TaskId");

            return GrantFirewallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .requestId(requestId)
                    .taskId(taskId)
                    .build();

        } catch (Exception e) {
            log.error("应用防火墙模板失败: ", e);
            return GrantFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    @Override
    public ClusterVO queryClusterList(ClusterListSO clusterListSO) throws Exception {
        return ClusterVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿里云-不支持此功能")
                .build();
    }

    /**
     * 查询模板列表
     * @param templateListSO
     * @return
     * @throws Exception
     */
    @Override
    public TemplateListVO queryTemplateList(TemplateListSO templateListSO) throws Exception {
        return TemplateListVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿里云-不支持此功能")
                .build();
    }

    /**
     * 订单支付
     * @param orderId
     * @return
     * @throws Exception
     */
    @Override
    public PayVO orderPay(String orderId) throws Exception {
        return PayVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿里云-不支持此功能")
                .build();
    }




    /**
     * 获取节点镜像信心
     * @return
     * @throws Exception
     */
    public JSONObject getSystem(){
        String acction = "ListImages";//修改密码

        Map<String,Object> param = new HashMap<>();
        param.put("RegionId", regionId);//区域ID
        param.put("ImageType","system");
        param.put("ClientToken", UUID.randomUUID().toString());

        try{
            JSONObject result = exec(acction,param);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }



    /**
     * 更新防火墙规则
     * @param updateFirewallSO
     * @return
     * @throws Exception
     */
    @Override
    public UpdateFirewallVO updateFirewall(UpdateFirewallSO updateFirewallSO) throws Exception {
        return UpdateFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("Aliyun-不支持此功能")
                .build();
    }

    /**
     * 删除防火墙
     * @param deleteFirewallSO
     * @return
     * @throws Exception
     */
    @Override
    public DeleteFirewallVO deleteFirewall(DeleteFirewallSO deleteFirewallSO) throws Exception {
        return DeleteFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("Aliyun-不支持此功能")
                .build();
    }

    /**
     * 解绑主机的所有安全组并清理无主防火墙
     * @param serviceNo
     * @return
     * @throws Exception
     */
    @Override
    public RevokeFirewallVO unbindAndCleanFirewalls(String serviceNo) throws Exception {
        return RevokeFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("Aliyun-不支持此功能")
                .build();
    }
}
