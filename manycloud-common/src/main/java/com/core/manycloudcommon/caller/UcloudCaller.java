package com.core.manycloudcommon.caller;

import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.entity.FirewallRule;
import com.core.manycloudcommon.enums.PowerStateEnum;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.HttpRequest;
import com.core.manycloudcommon.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.security.MessageDigest;
import java.util.*;

@Slf4j
public class UcloudCaller implements BaseCaller{


    private static Map<String,UcloudCaller> ucloudCallerMap = new HashMap<>();

    private String pubKey;
    private String pivKey;
    private String regionId;
    private String projectId;

    private String url;

    private UcloudCaller(String pubKey, String pivKey, String regionId, String url,String projectId){
        this.pivKey = pivKey;
        this.pubKey = pubKey;
        this.regionId = regionId;
        this.url = url;
        this.projectId = projectId;
    }





    /**
     * 获取客户端
     * @param accountApi
     * @return
     * @throws Exception
     */
    public static UcloudCaller getClient(AccountApi accountApi){

        if(ucloudCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId()) == null){
            synchronized(UcloudCaller.class){
                ucloudCallerMap.put(accountApi.getAccount()+":"+accountApi.getRegionId(),new UcloudCaller(accountApi.getKeyNo(),accountApi.getKeySecret(),accountApi.getRegionId(),accountApi.getBaseUrl(),accountApi.getProjectId()));
            }
            return ucloudCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }else{
            return ucloudCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }
    }




    /**
     * 创建轻量级主机
     * @param createSO
     * @return
     * @throws Exception
     */
    public CreateVO create(CreateSO createSO)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","CreateULHostInstance");
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        param.put("ImageId",createSO.getImageId());//镜像ID（CentOS 7.6 64位）
        param.put("BundleId",createSO.getBundleId());//固定折扣套餐
        param.put("Password",Base64.getEncoder().encodeToString(createSO.getPwd().getBytes()));//主机密码（需要同时包含两项及以上：大写字母，小写字母，数字，符号）
        if(StringUtils.isNotEmpty(createSO.getMachineType())){
            param.put("ChargeType",createSO.getMachineType());//ISP IP资源
        }else{
            param.put("ChargeType","Month");//付费类型：Year-按年付费,Month-按月付费
        }
        String securityGroupId = createSO.getSecurityGroupId();
        if(StringUtils.isNotEmpty(securityGroupId)){
            param.put("SecurityGroupId",securityGroupId);
        }

        param.put("Quantity",createSO.getPeriod()+"");//周期
        //param.put("CouponId","");//主机代金券ID
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            String instanceId = json.getString("ULHostId");
            return CreateVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .instanceIds(Arrays.asList(instanceId))
                    .build();
        }else{
            log.info("ucloud-创建轻量级云主机失败：{}",str);
            return CreateVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }

    }


    /**
     * 查询主机信息
     * @param querySO
     * @throws Exception
     */
    public QueryVO createQuery(QuerySO querySO)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","DescribeULHostInstance");
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);//美国洛杉矶
        List<String> instanceIds = querySO.getInstanceIds();
        for(int i = 0 ; i < instanceIds.size() ; i++){
            param.put("ULHostIds."+i,instanceIds.get(i));
        }
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        //
        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            JSONArray instances = json.getJSONArray("ULHostInstanceSets");
            Map<String ,QueryDetailVO> queryDetailMap = new HashMap<>();
            for(Object obj : instances){
                JSONObject instance = JSONObject.fromObject(obj);
                /** 实例状态。枚举值：
                 >初始化: Initializing;>启动中: Starting;> 运行中: Running;> 关机中: Stopping;>关机: Stopped>安装失败: Install Fail;>重启中: Rebooting;
                 **/
                String state = instance.getString("State");
                String serviceNo = instance.getString("ULHostId");
                String powerState;
                if("Initializing".toLowerCase().equals(state.toLowerCase()) || "Starting".toLowerCase().equals(state.toLowerCase())
                        || "Stopping".toLowerCase().equals(state.toLowerCase()) || "Rebooting".toLowerCase().equals(state.toLowerCase())){
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
                if(state.toLowerCase().equals("Running".toLowerCase())){//主机状态运行中（创建成功）

                    String ip = null;
                    String privateIp = null;
                    JSONArray ipSet = instance.getJSONArray("IPSet");

                    for(Object ipObj : ipSet){
                        JSONObject ipJson = JSONObject.fromObject(ipObj);
                        if("Resident".equals(ipJson.getString("Type"))){
                            ip = ipJson.getString("IP");
                            continue;
                        }else if("International".equals(ipJson.getString("Type"))){
                            ip = ipJson.getString("IP");
                            continue;
                        }
                        if("Private".equals(ipJson.getString("Type"))){
                            privateIp = ipJson.getString("IP");
                            continue;
                        }
                    }

                    if("Yes".equals(instance.getString("AutoRenew"))){//更新主机不自动续费
                        UpdateAuteRenewSO urs = UpdateAuteRenewSO.builder()
                                .instanceId(serviceNo)
                                .tad(1)
                                .build();
                        updateAuteRenew(urs);
                    }
                    String osType = instance.getString("OsType").toLowerCase();
                    String account = null;
                    Integer port = null;
                    if("windows".equals(osType.toLowerCase())){
                        account = "administrator";
                        port = 3389;
                    }else if("ubuntu".equals(osType.toLowerCase())){
                        account = "ubuntu";
                        port = 22;
                    }else if("centos".equals(osType.toLowerCase()) || "debian".equals(osType.toLowerCase()) || "docky".equals(osType.toLowerCase())){
                        account = "root";
                        port = 22;
                    }

                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(serviceNo)
                            .account(account)
                            .port(port)
                            .publicIp(ip)
                            .privateIp(privateIp)
                            .status(1)
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(serviceNo,odvo);

                }else if(state.indexOf("Fail") > -1) {//主机创建失败

                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(serviceNo)
                            .status(2)
                            .msg("Ucloud实例创建失败状态:"+state)
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(serviceNo,odvo);
                }else {
                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(serviceNo)
                            .status(0)
                            .msg("Ucloud实例创建等待状态:"+state)
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
        }else{
            log.info("ucloud-轻量级云主机{}查询实例失败：{}",JSONArray.fromObject(querySO.getInstanceIds()).toString(),str);
            return QueryVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }

    }


    /**
     * 查询主机信息
     * @param querySO
     * @throws Exception
     */
    public QueryVO query(QuerySO querySO)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","DescribeULHostInstance");
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);//美国洛杉矶
        List<String> instanceIds = querySO.getInstanceIds();
        for(int i = 0 ; i < instanceIds.size() ; i++){
            param.put("ULHostIds."+i,instanceIds.get(i));
        }
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        //
        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            JSONArray instances = json.getJSONArray("ULHostInstanceSets");
            Map<String ,QueryDetailVO> queryDetailMap = new HashMap<>();
            for(Object obj : instances){
                JSONObject instance = JSONObject.fromObject(obj);
                /** 实例状态。枚举值：
                 >初始化: Initializing;>启动中: Starting;> 运行中: Running;> 关机中: Stopping;>关机: Stopped>安装失败: Install Fail;>重启中: Rebooting;
                 **/
                String state = instance.getString("State");
                String serviceNo = instance.getString("ULHostId");
                String powerState;
                if("Initializing".toLowerCase().equals(state.toLowerCase()) || "Starting".toLowerCase().equals(state.toLowerCase())
                        || "Stopping".toLowerCase().equals(state.toLowerCase()) || "Rebooting".toLowerCase().equals(state.toLowerCase())){
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


                String ip = null;
                String privateIp = null;
                JSONArray ipSet = instance.getJSONArray("IPSet");

                for(Object ipObj : ipSet){
                    JSONObject ipJson = JSONObject.fromObject(ipObj);
                    if("International".equals(ipJson.getString("Type"))){
                        ip = ipJson.getString("IP");
                        continue;
                    }
                    if("Private".equals(ipJson.getString("Type"))){
                        privateIp = ipJson.getString("IP");
                        continue;
                    }
                }


                String osType = instance.getString("OsType").toLowerCase();
                String account = null;
                Integer port = null;
                if("windows".equals(osType)){
                    account = "administrator";
                    port = 3389;
                }else if("ubuntu".equals(osType)){
                    account = "ubuntu";
                    port = 22;
                }else if("centos".equals(osType) || "debian".equals(osType) || "docky".equals(osType)){
                    account = "root";
                    port = 22;
                }

                QueryDetailVO odvo = QueryDetailVO.builder()
                        .serviceNo(serviceNo)
                        .account(account)
                        .port(port)
                        .publicIp(ip)
                        .privateIp(privateIp)
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
        }else{
            log.info("ucloud-轻量级云主机{}查询实例失败：{}",JSONArray.fromObject(querySO.getInstanceIds()).toString(),str);
            return QueryVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }

    }

    /**
     * 续费
     * @param renewSO
     * @return
     * @throws Exception
     */
    public RenewVO renew(RenewSO renewSO)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","CreateRenew");
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("PublicKey",pubKey);
        param.put("ResourceId",renewSO.getInstanceId());
        param.put("Quantity",renewSO.getNum()+"");
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        //log.info("请求参数："+ JSONObject.fromObject(param).toString());
        String str = HttpRequest.post(url,param);
        //log.info("响应结果："+str);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            return RenewVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("ucloud-轻量级云主机[{}]实例续费失败：{}",renewSO.getInstanceId(),str);
            return RenewVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

//    /**
//     * 创建防火墙
//     * @param port 端口
//     * @return
//     */
//    public String createFirewall(String name,int port) throws Exception{
//        Map<String,String> param = new TreeMap<>();
//        param.put("Action","CreateFirewall");
//        param.put("PublicKey",pubKey);
//        param.put("Region",regionId);
//        if(StringUtils.isNotEmpty(projectId)){
//            param.put("ProjectId",projectId);
//        }
//        param.put("Name",name);
//        param.put("Rule.0","TCP|22|0.0.0.0/0|ACCEPT|HIGH|开的TCP22端口");
//        param.put("Rule.1","UDP|22|0.0.0.0/0|ACCEPT|HIGH|开的UDP22端口");
//        param.put("Rule.2","TCP|"+port+"|0.0.0.0/0|ACCEPT|HIGH|开的TCP"+port+"端口");
//        param.put("Rule.3","UDP|"+port+"|0.0.0.0/0|ACCEPT|HIGH|开的UDP"+port+"端口");
//        String signature =  getSignature(param,pivKey);
//        param.put("Signature",signature);
//        //log.info("请求参数："+ JSONObject.fromObject(param).toString());
//        String str = HttpRequest.post(url,param);
//        //log.info("响应结果："+str);
//        JSONObject json = JSONObject.fromObject(str);
//        if(json.getInt("RetCode") == 0){
//            return json.getString("FWId");
//        }else{
//            log.info("ucloud-创建防火墙{}失败：{}",name,str);
//            return null;
//        }
//    }

    /**
     * 创建防火墙
     * @param createSecuritySO
     * @return
     * @throws Exception
     */
    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO) throws Exception {
        // 先获取端口字符串并校验格式和范围
        String portStr = createSecuritySO.getPort();
        validatePort(portStr); // 调用校验方法

        Map<String, String> param = new TreeMap<>();
        param.put("Action", "CreateFirewall");
        param.put("PublicKey", pubKey);
        param.put("Region", regionId);
        if (StringUtils.isNotEmpty(projectId)) {
            param.put("ProjectId", projectId);
        }
        param.put("Name", createSecuritySO.getName());

        // 解析动态端口，生成规则
        List<String> dynamicPorts = parsePortString(portStr); // 解析端口字符串
        int ruleIndex = 0; // 从 Rule.0 开始添加规则

        for (String port : dynamicPorts) {
            // 添加 TCP 和 UDP 规则
            param.put("Rule." + ruleIndex++, "TCP|" + port + "|0.0.0.0/0|ACCEPT|HIGH|开的TCP" + port + "端口");
            param.put("Rule." + ruleIndex++, "UDP|" + port + "|0.0.0.0/0|ACCEPT|HIGH|开的UDP" + port + "端口");
        }

        // 生成签名并调用 API
        String signature = getSignature(param, pivKey);
        param.put("Signature", signature);
        String str = HttpRequest.post(url, param);
        JSONObject json = JSONObject.fromObject(str);

        if (json.getInt("RetCode") == 0) {
            return CreateSecurityVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .fwId(json.getString("FWId"))
                    .build();
        } else {
            log.info("ucloud-创建防火墙{}失败：{}", createSecuritySO.getName(), str);
            return null;
        }
}

    /**
     * 校验端口格式合法性（支持单个端口、多个端口、端口范围）
     */
    private void validatePort(String portStr) {
        // 非空校验
        if (StringUtils.isEmpty(portStr)) {
            throw new IllegalArgumentException("端口不能为空");
        }

        // 按逗号分割多个端口/范围
        String[] portItems = portStr.split(",");
        for (String item : portItems) {
            item = item.trim();
            if (item.isEmpty()) {
                throw new IllegalArgumentException("端口格式错误，存在空项：" + portStr);
            }

            // 按短横线分割范围端口
            String[] rangeParts = item.split("-");
            if (rangeParts.length > 2) {
                throw new IllegalArgumentException("端口格式错误，范围只能包含起始和结束值：" + item);
            }

            // 校验每个部分是否为合法端口（1-65535）
            try {
                for (String part : rangeParts) {
                    int port = Integer.parseInt(part.trim());
                    if (port < 1 || port > 65535) {
                        throw new IllegalArgumentException("端口必须在1-65535之间，当前值：" + port);
                    }
                }
                // 校验范围的起始值 <= 结束值
                if (rangeParts.length == 2) {
                    int start = Integer.parseInt(rangeParts[0].trim());
                    int end = Integer.parseInt(rangeParts[1].trim());
                    if (start > end) {
                        throw new IllegalArgumentException("端口范围起始值不能大于结束值：" + item);
                    }
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("端口必须为数字或数字范围，当前值：" + item);
            }
        }
    }

    /**
     * 解析端口字符串为端口列表（去重）
     */
    private List<String> parsePortString(String portStr) {
        Set<String> portSet = new LinkedHashSet<>(); // 去重且保留顺序
        String[] portItems = portStr.split(",");
        for (String item : portItems) {
            portSet.add(item.trim()); // 直接保留原始格式
        }
        return new ArrayList<>(portSet);
    }



    /**
     * 查询防火墙
     * @param queryFirewallSO
     * @return
     * @throws Exception
     */
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception {

        String groupId = null;
        String fwId = null;
        String fwName = null;
        List<FirewallRule> rules = new ArrayList<>(); // 用于存储规则信息（协议、端口等）

        Map<String,String> param = new TreeMap<>();
        param.put("Action","DescribeFirewall");
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Limit","10000");
        if(StringUtils.isNotEmpty(queryFirewallSO.getFwId())){
            param.put("FWId",queryFirewallSO.getFwId());
        }
        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);

        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);

        if(json.getInt("RetCode") == 0){
            JSONArray dataArray = json.getJSONArray("DataSet");
            for(int i = 0 ; i < dataArray.size() ; i++){
                JSONObject data = dataArray.getJSONObject(i);
                boolean isMatch = false;
                if(StringUtils.isNotEmpty(queryFirewallSO.getFwId())){
                    isMatch = queryFirewallSO.getFwId().equals(data.getString("FWId"));
                }else{
                    isMatch = queryFirewallSO.getName().equals(data.getString("Name"));
                }
                if (isMatch) {
                    groupId = data.getString("GroupId");
                    fwId = data.getString("FWId");
                    fwName = data.getString("Name");

                    // 解析规则数组，提取协议和端口
                    JSONArray ruleArray = data.optJSONArray("Rule");
                    if (ruleArray != null) {
                        for (int j = 0; j < ruleArray.size(); j++) {
                            JSONObject rule = ruleArray.getJSONObject(j);
                            FirewallRule ruleInfo = new FirewallRule();
                            ruleInfo.setProtocol(rule.optString("ProtocolType"));
                            ruleInfo.setPort(rule.optString("DstPort"));
                            ruleInfo.setAction(rule.optString("RuleAction"));
                            ruleInfo.setPriority(rule.optString("Priority"));
                            ruleInfo.setIpAddress(rule.optString("SrcIP"));
                            rules.add(ruleInfo);
                        }
                    }
                    break;
                }
            }
        }
        return QueryFirewallVO.builder()
                .groupId(groupId)
                .fwId(fwId)
                .name(fwName)
                .rules(rules) // 将解析出的规则信息设置到返回对象中
                .code(CommonUtil.SUCCESS_CODE)
                .msg("查询成功")
                .build();
    }


//    /**
//     * 查询防火墙
//     * @return
//     */
//    public String queryFirewall(String name,String fwId) throws Exception{
//
//        String groupId = null;
//        Map<String,String> param = new TreeMap<>();
//        param.put("Action","DescribeFirewall");
//        param.put("PublicKey",pubKey);
//        param.put("Region",regionId);
//        if(StringUtils.isNotEmpty(projectId)){
//            param.put("ProjectId",projectId);
//        }
//        param.put("Limit","10000");
//        if(StringUtils.isNotEmpty(fwId)){
//            param.put("FWId",fwId);
//        }
//        String signature =  getSignature(param,pivKey);
//        param.put("Signature",signature);
//        //log.info("请求参数："+ JSONObject.fromObject(param).toString());
//        String str = HttpRequest.post(url,param);
//        //log.info("响应结果："+str);
//
//        JSONObject json = JSONObject.fromObject(str);
//        if(json.getInt("RetCode") == 0){
//            JSONArray dataArray = json.getJSONArray("DataSet");
//            for(int i = 0 ; i < dataArray.size() ; i++){
//                JSONObject data = dataArray.getJSONObject(i);
//                if(StringUtils.isNotEmpty(fwId)){
//                    if(fwId.equals(data.getString("FWId"))){
//                        groupId = data.getString("GroupId");
//                        break;
//                    }
//                }else{
//                    if(name.equals(data.getString("Name"))){
//                        groupId = data.getString("GroupId");
//                        break;
//                    }
//                }
//
//            }
//        }
//        return groupId;
//    }
    /**
     * 绑定防火墙
     * @param grantFirewallSO
     * @return
     * @throws Exception
     */
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO) throws Exception {
        Map<String,String> param = new TreeMap<>();
        //param.put("Action","GrantFirewall");
        param.put("Action","GrantSecurityGroup");
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        //param.put("FWId",fwId);
        param.put("GroupId",grantFirewallSO.getGroupId());
        param.put("ResourceType","ulhost");
        param.put("ResourceId",grantFirewallSO.getInstanceId());
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        //log.info("请求参数："+ JSONObject.fromObject(param).toString());
        String str = HttpRequest.post(url,param);
        //log.info("响应结果："+str);
        JSONObject json = JSONObject.fromObject(str);
        return GrantFirewallVO.builder()
                .success(json.getInt("RetCode") == 0)
                .code(json.getInt("RetCode") == 0 ? CommonUtil.SUCCESS_CODE : CommonUtil.FAIL_CODE)
                .msg(json.getInt("RetCode") == 0 ? "绑定成功" : "绑定失败: " + str)
                .build();
    }

    /**
     * 查询集群列表
     * @param clusterListSO
     * @return
     * @throws Exception
     */
    @Override
    public ClusterVO queryClusterList(ClusterListSO clusterListSO) throws Exception {
        return ClusterVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("Ucloud-不支持此功能")
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
                .msg("Ucloud-不支持此功能")
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
                .msg("Ucloud-不支持此功能")
                .build();
    }

    /**
     * 开机
     * @param startSO
     * @return
     * @throws Exception
     */
    public StartVO start(StartSO startSO)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","StartUHostInstance");
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("PublicKey",pubKey);

        param.put("Region",regionId);//地域
        param.put("UHostId",startSO.getInstanceId());//主机ID

        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);


        String str = HttpRequest.postJson(url,JSONObject.fromObject(param).toString(),null);
        JSONObject result = JSONObject.fromObject(str);
        if(result.getInt("RetCode") == 0){
            return StartVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("ucloud-轻量级云主机[{}]开机失败：{}",startSO.getInstanceId(),str);
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
    public RebootVO reboot(RebootSO rebootSO)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","RebootUHostInstance");
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("PublicKey",pubKey);

        /*if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }*/
        param.put("Region",regionId);//地域
        param.put("UHostId",rebootSO.getInstanceId());//主机ID

        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);

        String str = HttpRequest.postJson(url,JSONObject.fromObject(param).toString(),null);

        JSONObject result = JSONObject.fromObject(str);
        if(result.getInt("RetCode") == 0){
            return RebootVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("ucloud-轻量级云主机[{}]重启失败：{}",rebootSO.getInstanceId(),str);
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
    public StopVO stop(StopSO stopSO)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","StopULHostInstance");
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        param.put("ULHostId",stopSO.getInstanceId());
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        //log.info("请求参数："+ JSONObject.fromObject(param).toString());
        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            return StopVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("ucloud-轻量级云主机{} 关机失败：{}",stopSO.getInstanceId(),str);
            return StopVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 设置主机自动续费标识
     * @param updateAuteRenewSO
     * @return
     */
    public UpdateAuteRenewVO updateAuteRenew(UpdateAuteRenewSO updateAuteRenewSO)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","ModifyAutoRenewFlag");
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("ResourceId",updateAuteRenewSO.getInstanceId());
        if(updateAuteRenewSO.getTad() == 0){
            param.put("Flag","TURN_ON");
        }else if(updateAuteRenewSO.getTad() == 1){
            param.put("Flag","TURN_OFF");
        }

        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        //log.info("请求参数："+ JSONObject.fromObject(param).toString());
        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            return UpdateAuteRenewVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("ucloud-轻量级云主机{} 修改自动续费[{}]失败：{}",updateAuteRenewSO.getInstanceId(),updateAuteRenewSO.getTad(),str);
            return UpdateAuteRenewVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 更新主机密码
     * @param updatePwdSO
     * @return
     * @throws Exception
     */
    public UpdatePwdVO updatePwd(UpdatePwdSO updatePwdSO) throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","ResetUHostInstancePassword");//更新主机密码
        param.put("PublicKey",pubKey);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//地域
        param.put("UHostId",updatePwdSO.getInstanceId());//主机ID
        param.put("Password",Base64.getEncoder().encodeToString(updatePwdSO.getPwd().getBytes()));//新密码

        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);


        String str = HttpRequest.postJson(url,JSONObject.fromObject(param).toString(),null);

        JSONObject result = JSONObject.fromObject(str);
        if(result.getInt("RetCode") == 0){
            return UpdatePwdVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("ucloud-轻量级云主机[{}]修改密码失败：{}",updatePwdSO.getInstanceId(),str);
            return UpdatePwdVO.builder()
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
    public ReinstallVO reinstall(ReinstallSO reinstallSO) throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","ReinstallULHostInstance");//重装系统
        param.put("PublicKey",pubKey);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//地域
        param.put("ULHostId",reinstallSO.getInstanceId());//主机ID
        param.put("ImageId",reinstallSO.getImageId());
        param.put("Password",Base64.getEncoder().encodeToString(reinstallSO.getPwd().getBytes()));//密码

        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);


        String str = HttpRequest.postJson(url,JSONObject.fromObject(param).toString(),null);

        JSONObject result = JSONObject.fromObject(str);
        if(result.getInt("RetCode") == 0){
            return ReinstallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("ucloud-轻量级云主机{} 重装失败：{}",reinstallSO.getInstanceId(),str);
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
    public DestroyVO destroy(DestroySO destroySO)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","TerminateULHostInstance");
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("ULHostId",destroySO.getInstanceId());
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        //log.info("请求参数："+ JSONObject.fromObject(param).toString());
        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            return DestroyVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("ucloud-轻量级云主机{} 销毁失败：{}",destroySO.getInstanceId(),str);
            return DestroyVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 获取镜像
     * @return
     */
    public String getImage(){
        Map<String,String> param = new TreeMap<>();
        param.put("Action","DescribeImage");
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("ImageType","ImageType");
        param.put("Limit","200");
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        try {
            String str = HttpRequest.post(url,param);
            return str;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }







    /**
     * 签名
     * @param param 请求参数
     * @param privateKey 私钥
     * @return
     */
    public  String getSignature(Map<String,String> param,String privateKey){
        StringBuilder stringBuilder = new StringBuilder();

        for(String key : param.keySet()){
            stringBuilder.append(key+param.get(key));
        }
        stringBuilder.append(privateKey);
        return getSha1(stringBuilder.toString());
    }

    /**
     * SHA1加密
     * @param str
     * @return
     */
    public  String getSha1(String str) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9','a', 'b', 'c', 'd', 'e', 'f' };

        try {
            MessageDigest mdTemp = MessageDigest.getInstance("SHA1");

            mdTemp.update(str.getBytes("UTF-8"));

            byte[] md = mdTemp.digest();

            int j = md.length;

            char buf[] = new char[j * 2];

            int k = 0;

            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];

                buf[k++] = hexDigits[byte0 >>> 4 & 0xf];

                buf[k++] = hexDigits[byte0 & 0xf];

            }

            return new String(buf);

        } catch (Exception e) {
            return null;

        }

    }
}
