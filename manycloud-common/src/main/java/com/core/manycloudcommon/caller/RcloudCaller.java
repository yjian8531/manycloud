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

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;

/**
 * R平台(底层是Ucloud)
 */
@Slf4j
public class RcloudCaller implements BaseCaller{


    private static Map<String, RcloudCaller> rcloudCallerMap = new HashMap<>();

    private String pubKey;
    private String pivKey;
    private String regionId;
    private String projectId;

    private String url;

    private RcloudCaller(String pubKey, String pivKey, String regionId, String url, String projectId){
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
    public static RcloudCaller getClient(AccountApi accountApi){

        if(rcloudCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId()) == null){
            synchronized(RcloudCaller.class){
                rcloudCallerMap.put(accountApi.getAccount()+":"+accountApi.getRegionId(),new RcloudCaller(accountApi.getKeyNo(),accountApi.getKeySecret(),accountApi.getRegionId(),accountApi.getBaseUrl(),accountApi.getProjectId()));
            }
            return rcloudCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }else{
            return rcloudCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }
    }




    /**
     * 创建云主机
     * @param createSO
     * @return
     * @throws Exception
     */
    public CreateVO create(CreateSO createSO)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","CreateUHostInstance");//创建主机
        param.put("PublicKey",pubKey);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//广州
        param.put("Zone",createSO.getZone());//广州可用区B
        param.put("ImageId",createSO.getImageId());//镜像ID
        param.put("Disks.0.IsBoot","True");//是否系统盘
        param.put("Disks.0.Type",createSO.getDisksType());//"LOCAL_NORMAL","CLOUD_RSSD"
        param.put("Disks.0.Size",createSO.getDisksSize()+"");

        param.put("LoginMode","Password");//登录类型-密码
        param.put("Password", Base64.getEncoder().encodeToString(createSO.getPwd().getBytes()));
        param.put("ChargeType","Month");//Year，按年付费  Month，按月付费 Dynamic，按小时预付费
        param.put("Quantity",createSO.getPeriod()+"");//购买周期
        param.put("CPU",createSO.getCpu()+"");//
        int memory = new BigDecimal(createSO.getRam()).multiply(BigDecimal.valueOf(1024)).intValue();
        param.put("Memory",memory +"");//2G
        param.put("MachineType",createSO.getMachineType());//快捷共享型
        param.put("MinimalCpuPlatform","Intel/Auto");//AMD

        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);

        String str = HttpRequest.post(url,param);


        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            JSONArray uhostIds = json.getJSONArray("UHostIds");
            if(uhostIds.size() > 0){
                return CreateVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .instanceIds(Arrays.asList(uhostIds.getString(0)))
                        .build();
            }else{
                log.info("rcloud-创建云主机失败：{}",str);
                return CreateVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(CommonUtil.FAIL_MSG)
                        .build();
            }

        }else{
            log.info("rcloud-创建云主机失败：{}",str);
            return CreateVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }

    }

    /**
     * 查询主机创建信息
     * @param querySO
     * @throws Exception
     */
    public QueryVO createQuery(QuerySO querySO)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","DescribeUHostInstance");//查询主机
        param.put("PublicKey",pubKey);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//广州
        param.put("UHostIds.0",querySO.getInstanceIds().get(0));//主机编号

        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);

        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){

            JSONArray instances = json.getJSONArray("UHostSet");
            Map<String ,QueryDetailVO> queryDetailMap = new HashMap<>();
            for(Object obj : instances){
                JSONObject instance = JSONObject.fromObject(obj);
                /** 实例状态。枚举值：
                 >初始化: Initializing;>启动中: Starting;> 运行中: Running;> 关机中: Stopping;>关机: Stopped>安装失败: Install Fail;>重启中: Rebooting;
                 **/
                String state = instance.getString("State");
                String serviceNo = instance.getString("UHostId");
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
                        if("Native".equals(ipJson.getString("Type"))){//原生IP
                            ip = ipJson.getString("IP");
                            continue;
                        }
                        if("Private".equals(ipJson.getString("Type"))){
                            privateIp = ipJson.getString("IP");
                            continue;
                        }
                    }

                    if(ip == null){

                        /** 创建一个绑定共享带宽的EIP **/
                        Map<String,String> eipMap = createEip(querySO.getShareId(),1);
                        if (eipMap != null){

                            String publicIpId = eipMap.get("id");
                            ip = eipMap.get("ip");

                            //eip绑定实例
                            bindIp(publicIpId,serviceNo);
                        }

                    }


                    if("Yes".equals(instance.getString("AutoRenew"))){//更新主机不自动续费
                        UpdateAuteRenewSO urs = UpdateAuteRenewSO.builder()
                                .instanceId(serviceNo)
                                .tad(1)
                                .build();
                        updateAuteRenew(urs);
                    }
                    String osName = instance.getString("OsName").toLowerCase();
                    String account = null;
                    Integer port = null;
                    if(osName.indexOf("windows") > -1){
                        account = "administrator";
                        port = 3389;
                    }else if(osName.indexOf("ubuntu") > -1){
                        account = "ubuntu";
                        port = 22;
                    }else if(osName.indexOf("centos") > -1 || osName.indexOf("debian") > -1 || osName.indexOf("docky") > -1){
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
                            .msg("Rcloud实例创建失败状态:"+state)
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(serviceNo,odvo);
                }else {
                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(serviceNo)
                            .status(0)
                            .msg("Rcloud实例创建等待状态:"+state)
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
            log.info("rcloud-云主机{}查询实例失败：{}",JSONArray.fromObject(querySO.getInstanceIds()).toString(),str);
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
        param.put("Action","DescribeUHostInstance");//查询主机
        param.put("PublicKey",pubKey);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//广州
        param.put("UHostIds.0",querySO.getInstanceIds().get(0));//主机编号

        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);

        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            JSONArray instances = json.getJSONArray("UHostSet");
            Map<String ,QueryDetailVO> queryDetailMap = new HashMap<>();
            for(Object obj : instances){
                JSONObject instance = JSONObject.fromObject(obj);
                /** 实例状态。枚举值：
                 >初始化: Initializing;>启动中: Starting;> 运行中: Running;> 关机中: Stopping;>关机: Stopped>安装失败: Install Fail;>重启中: Rebooting;
                 **/
                String state = instance.getString("State");
                String serviceNo = instance.getString("UHostId");
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
                    if("Native".equals(ipJson.getString("Type"))){//原生IP
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
        param.put("PublicKey",pubKey);
        param.put("ResourceId",renewSO.getInstanceId());
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Quantity",renewSO.getNum()+"");
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);

        String str = HttpRequest.post(url,param);

        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            return RenewVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("Rcloud-轻量级云主机[{}]实例续费失败：{}",renewSO.getInstanceId(),str);
            return RenewVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 创建防火墙
     * @param port 端口
     * @return
     */
    public String createFirewall(String name,int port) throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","CreateFirewall");
        param.put("PublicKey",pubKey);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);
        param.put("Name",name);
        param.put("Rule.0","TCP|22|0.0.0.0/0|ACCEPT|HIGH|开的TCP22端口");
        param.put("Rule.1","UDP|22|0.0.0.0/0|ACCEPT|HIGH|开的UDP22端口");
        param.put("Rule.2","TCP|"+port+"|0.0.0.0/0|ACCEPT|HIGH|开的TCP"+port+"端口");
        param.put("Rule.3","UDP|"+port+"|0.0.0.0/0|ACCEPT|HIGH|开的UDP"+port+"端口");
        param.put("Rule.4","TCP|3389|0.0.0.0/0|ACCEPT|HIGH|开的TCP3389端口");
        param.put("Rule.5","UDP|3389|0.0.0.0/0|ACCEPT|HIGH|开的UDP3389端口");
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        //log.info("请求参数："+ JSONObject.fromObject(param).toString());
        String str = HttpRequest.post(url,param);
        //log.info("响应结果："+str);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            return json.getString("FWId");
        }else{
            log.info("Rcloud-创建防火墙{}失败：{}",name,str);
            return null;
        }
    }

    /**
     * 查询防火墙
     * @return
     */
    public String queryFirewall(String name,String fwId) throws Exception{

        String groupId = null;
        Map<String,String> param = new TreeMap<>();
        param.put("Action","DescribeFirewall");
        param.put("PublicKey",pubKey);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);
        param.put("Limit","10000");
        if(StringUtils.isNotEmpty(fwId)){
            param.put("FWId",fwId);
        }
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);

        String str = HttpRequest.post(url,param);


        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            JSONArray dataArray = json.getJSONArray("DataSet");
            for(int i = 0 ; i < dataArray.size() ; i++){
                JSONObject data = dataArray.getJSONObject(i);
                if(StringUtils.isNotEmpty(fwId)){
                    if(fwId.equals(data.getString("FWId"))){
                        groupId = data.getString("GroupId");
                        break;
                    }
                }else if(StringUtils.isNotEmpty(name)){
                    if(data.getString("Name").indexOf(name) > -1){
                        groupId = data.getString("GroupId");
                        break;
                    }
                }

            }
        }else {
            log.info("Rcloud查防火墙请求参数："+ JSONObject.fromObject(param).toString());
            log.info("Rcloud查防火墙响应结果："+str);
        }
        return groupId;
    }

    /**
     * 关联防火墙信息
     * @param groupId 防火墙ID(安全组ID)
     * @param instanceId 主机ID
     * @return
     */
    public boolean grantFirewall(String groupId,String instanceId) throws Exception{
        Map<String,String> param = new TreeMap<>();
        //param.put("Action","GrantFirewall");
        param.put("Action","GrantSecurityGroup");
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        //param.put("FWId",fwId);
        param.put("GroupId",groupId);
        param.put("ResourceType","ulhost");
        param.put("ResourceId",instanceId);
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        //log.info("请求参数："+ JSONObject.fromObject(param).toString());
        String str = HttpRequest.post(url,param);
        //log.info("响应结果："+str);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            return true;
        }else{
            log.info("ucloud-关联防火墙信息："+str);
            return false;
        }
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
        param.put("PublicKey",pubKey);

        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//地域
        param.put("UHostId",startSO.getInstanceId());//主机ID

        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);


        String str = HttpRequest.post(url,param);
        JSONObject result = JSONObject.fromObject(str);
        if(result.getInt("RetCode") == 0){
            return StartVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("Rcloud-云主机[{}]开机失败：{}",startSO.getInstanceId(),str);
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
        param.put("PublicKey",pubKey);

        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//地域
        param.put("UHostId",rebootSO.getInstanceId());//主机ID

        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);

        String str = HttpRequest.post(url,param);

        JSONObject result = JSONObject.fromObject(str);
        if(result.getInt("RetCode") == 0){
            return RebootVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("Rcloud-云主机[{}]重启失败：{}",rebootSO.getInstanceId(),str);
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
        param.put("Action","StopUHostInstance");
        param.put("PublicKey",pubKey);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//地域
        param.put("UHostId",stopSO.getInstanceId());//主机ID

        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);


        String str = HttpRequest.post(url,param);

        JSONObject result = JSONObject.fromObject(str);
        if(result.getInt("RetCode") == 0){
            return StopVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("Rcloud-云主机{} 关机失败：{}",stopSO.getInstanceId(),str);
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
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//地域
        param.put("ResourceId",updateAuteRenewSO.getInstanceId());
        if(updateAuteRenewSO.getTad() == 0){
            param.put("Flag","TURN_ON");
        }else if(updateAuteRenewSO.getTad() == 1){
            param.put("Flag","TURN_OFF");
        }

        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);

        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            return UpdateAuteRenewVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("Rcloud-云主机{} 修改自动续费[{}]失败：{}",updateAuteRenewSO.getInstanceId(),updateAuteRenewSO.getTad(),str);
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
            log.info("Rcloud-云主机[{}]修改密码失败：{}",updatePwdSO.getInstanceId(),str);
            return UpdatePwdVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }
    /**
     * 创建防火墙
     * @param createSecuritySO
     * @return
     * @throws Exception
     */

    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO) throws Exception {
        Map<String, String> param = new TreeMap<>();
        param.put("Action", "CreateFirewall");
        param.put("PublicKey", pubKey);
        param.put("Region", regionId);
        if (StringUtils.isNotEmpty(projectId)) {
            param.put("ProjectId", projectId);
        }
        param.put("Name", createSecuritySO.getName());

        int ruleIndex = 0;
        List<CreateSecuritySO.FirewallRule> rules = createSecuritySO.getRules();
        if (rules != null && !rules.isEmpty()) {
            // 新接口格式：按每条规则的协议/端口/源IP/描述生成
            for (CreateSecuritySO.FirewallRule rule : rules) {
                String protocol = StringUtils.isNotEmpty(rule.getProtocol()) ? rule.getProtocol().toUpperCase() : "TCP";
                String port = rule.getPort();
                validatePort(port);
                String source = StringUtils.isNotEmpty(rule.getSource()) ? rule.getSource() : "0.0.0.0/0";
                String remark = StringUtils.isNotEmpty(rule.getDescription()) ? rule.getDescription() : "开的" + protocol + port + "端口";
                // UCloud规则格式：协议|端口|源IP|动作|优先级|备注
                param.put("Rule." + ruleIndex++, protocol + "|" + port + "|" + source + "|ACCEPT|HIGH|" + remark);
            }
        } else {
            // 旧格式：仅传port字符串，默认TCP+UDP双协议
            String portStr = createSecuritySO.getPort();
            validatePort(portStr);
            List<String> dynamicPorts = parsePortString(portStr);
            for (String port : dynamicPorts) {
                param.put("Rule." + ruleIndex++, "TCP|" + port + "|0.0.0.0/0|ACCEPT|HIGH|开的TCP" + port + "端口");
                param.put("Rule." + ruleIndex++, "UDP|" + port + "|0.0.0.0/0|ACCEPT|HIGH|开的UDP" + port + "端口");
            }
        }

        String signature = getSignature(param, pivKey);
        param.put("Signature", signature);
        String str = HttpRequest.post(url, param);
        JSONObject json = JSONObject.fromObject(str);

        if (json.getInt("RetCode") == 0) {
            log.info("Rcloud-创建防火墙{}成功：{}", createSecuritySO.getName(), json.getString("FWId"));
            return CreateSecurityVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .fwId(json.getString("FWId"))
                    .build();
        } else {
            log.info("Rcloud-创建防火墙{}失败：{}", createSecuritySO.getName(), str);
            return null;
        }
    }

    /**
     * 校验端口格式合法性（支持单个端口、多个端口、端口范围）
     */
    private void validatePort(String portStr) {
        if (StringUtils.isEmpty(portStr)) {
            throw new IllegalArgumentException("端口不能为空");
        }

        String[] portItems = portStr.split(",");
        for (String item : portItems) {
            item = item.trim();
            if (item.isEmpty()) {
                throw new IllegalArgumentException("端口格式错误，存在空项：" + portStr);
            }

            String[] rangeParts = item.split("-");
            if (rangeParts.length > 2) {
                throw new IllegalArgumentException("端口格式错误，范围只能包含起始和结束值：" + item);
            }

            try {
                for (String part : rangeParts) {
                    int port = Integer.parseInt(part.trim());
                    if (port < 1 || port > 65535) {
                        throw new IllegalArgumentException("端口必须在1-65535之间，当前值：" + port);
                    }
                }
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
        Set<String> portSet = new LinkedHashSet<>();
        String[] portItems = portStr.split(",");
        for (String item : portItems) {
            portSet.add(item.trim());
        }
        return new ArrayList<>(portSet);
    }

    /**
     * 查询主机绑定的防火墙FWId集合（DescribeSecurityGroup按ResourceId查，取FWId字段，DisassociateFirewall需要FWId而非GroupId）。
     * 只返回用户自建的（Type非0的是平台官方模板，不动）
     */
    private Set<String> queryBoundGroupIds(String resourceId) {
        Map<String,String> fwIdTypeMap = queryBoundFirewalls(resourceId);
        Set<String> groupIds = new HashSet<>();
        for(Map.Entry<String,String> entry : fwIdTypeMap.entrySet()){
            String type = entry.getValue();
            // Type=0用户自建（可解绑）；Type=1等非0是官方推荐模板，不解绑（UCloud实测：官方Type=1，自建Type=0）
            if(!"0".equals(type)){
                log.info("Rcloud-主机{}绑定的防火墙{}为官方模板(Type={})，跳过不解绑", resourceId, entry.getKey(), type);
                continue;
            }
            groupIds.add(entry.getKey());
        }
        return groupIds;
    }

    /**
     * 查询主机绑定的防火墙及类型（FWId -> Type），DescribeSecurityGroup按ResourceId查
     * 同时维护FWId->GroupId映射（清理环节按GroupId精确查询用，Rcloud的DescribeFirewall不支持FWId.N批量查询）
     */
    private Map<String,String> fwIdGroupIdMap = new HashMap<>();

    private Map<String,String> queryBoundFirewalls(String resourceId) {
        Map<String,String> fwIdTypeMap = new HashMap<>();
        try {
            Map<String,String> param = new TreeMap<>();
            param.put("Action","DescribeSecurityGroup");
            param.put("PublicKey",pubKey);
            param.put("Region",regionId);
            if(StringUtils.isNotEmpty(projectId)){
                param.put("ProjectId",projectId);
            }
            param.put("ResourceType","uhost");
            param.put("ResourceId",resourceId);
            String signature = getSignature(param,pivKey);
            param.put("Signature",signature);
            String str = HttpRequest.post(url,param);
//            log.info("Rcloud-DescribeSecurityGroup响应：{}",str);
            JSONObject json = JSONObject.fromObject(str);
            if(json.getInt("RetCode") == 0){
                JSONArray dataSet = json.optJSONArray("DataSet");
                if(dataSet != null){
                    for(int i = 0; i < dataSet.size(); i++){
                        JSONObject data = dataSet.getJSONObject(i);
                        String fwId = data.optString("FWId");
                        if(StringUtils.isEmpty(fwId)){
                            fwId = data.optString("FirewallId");
                        }
                        if(StringUtils.isNotEmpty(fwId)){
                            // DescribeSecurityGroup返回的Type是数字（0=用户自建，1=官方推荐），统一转字符串处理
                            fwIdTypeMap.put(fwId, data.opt("Type") == null ? null : String.valueOf(data.opt("Type")));
                            String gid = data.optString("GroupId");
                            if(StringUtils.isNotEmpty(gid)){
                                fwIdGroupIdMap.put(fwId, gid);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info("Rcloud-查询主机绑定防火墙异常：{}", e.getMessage());
        }
        return fwIdTypeMap;
    }

    /**
     * 查询防火墙
     * @param queryFirewallSO
     * @return
     * @throws Exception
     */
    @Override
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception {
        String groupId = null;
        String fwId = null;
        String fwName = null;
        List<FirewallRule> rules = new ArrayList<>();

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
            // 按主机查询时：先调DescribeSecurityGroup拿该主机绑定的GroupId集合
            Set<String> boundGroupIds = null;
            if(StringUtils.isEmpty(queryFirewallSO.getFwId())
                    && StringUtils.isEmpty(queryFirewallSO.getName())
                    && StringUtils.isNotEmpty(queryFirewallSO.getInstanceId())){
                boundGroupIds = queryBoundGroupIds(queryFirewallSO.getInstanceId());
            }
            for(int i = 0 ; i < dataArray.size() ; i++){
                JSONObject data = dataArray.getJSONObject(i);
                boolean isMatch = false;
                if(StringUtils.isNotEmpty(queryFirewallSO.getFwId())){
                    isMatch = queryFirewallSO.getFwId().equals(data.getString("FWId"));
                }else if(StringUtils.isNotEmpty(queryFirewallSO.getName())){
                    isMatch = queryFirewallSO.getName().equals(data.getString("Name"));
                }else if(boundGroupIds != null){
                    // 该主机绑定的防火墙中包含此条
                    isMatch = boundGroupIds.contains(data.getString("GroupId"));
                }
                if (isMatch) {
                    groupId = data.getString("GroupId");
                    fwId = data.getString("FWId");
                    fwName = data.getString("Name");

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
                            ruleInfo.setRemark(rule.optString("Remark"));
                            rules.add(ruleInfo);
                        }
                    }
                    break;
                }
            }
        }else{
            log.info("Rcloud查防火墙失败：{}",str);
        }
        return QueryFirewallVO.builder()
                .groupId(groupId)
                .fwId(fwId)
                .name(fwName)
                .rules(rules)
                .code(CommonUtil.SUCCESS_CODE)
                .msg("查询成功")
                .build();
    }

    @Override
    public CreateFirewallTemplateRulesVO createFirewallTemplateRules(CreateFirewallTemplateRulesSO so) {
        return CreateFirewallTemplateRulesVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("Rcloud-不支持此功能")
                .build();
    }

    /**
     * 绑定防火墙
     * @param grantFirewallSO
     * @return
     * @throws Exception
     */
    @Override
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO) throws Exception {
        Map<String,String> param = new TreeMap<>();
        //param.put("Action","GrantFirewall");
        param.put("Action","GrantSecurityGroup");
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("GroupId",grantFirewallSO.getGroupId());
        param.put("ResourceType","uhost");// Rcloud主机是CreateUHostInstance创建的，资源类型为uhost（UCLOUD轻量才是ulhost）
        param.put("ResourceId",grantFirewallSO.getInstanceId());
        param.put("ResourceId",grantFirewallSO.getInstanceId());
        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);
        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            log.info("Rcloud-主机{}绑定防火墙{}成功", grantFirewallSO.getInstanceId(), grantFirewallSO.getGroupId());
        }else{
            log.info("Rcloud-主机{}绑定防火墙{}失败：{}", grantFirewallSO.getInstanceId(), grantFirewallSO.getGroupId(), str);
        }
        return GrantFirewallVO.builder()
                .success(json.getInt("RetCode") == 0)
                .code(json.getInt("RetCode") == 0 ? CommonUtil.SUCCESS_CODE : CommonUtil.FAIL_CODE)
                .msg(json.getInt("RetCode") == 0 ? "绑定成功" : "绑定失败: " + str)
                .build();
    }

    /**
     * 更新防火墙规则（UCloud的UpdateFirewall为全量覆盖，需传入旧规则+新规则）
     * @param updateFirewallSO
     * @return
     * @throws Exception
     */
    @Override
    public UpdateFirewallVO updateFirewall(UpdateFirewallSO updateFirewallSO) throws Exception {
        Map<String,String> param = new TreeMap<>();
        param.put("Action","UpdateFirewall");
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("FWId",updateFirewallSO.getFwId());
        List<String> rules = updateFirewallSO.getRules();
        if(rules != null){
            for(int i = 0; i < rules.size(); i++){
                param.put("Rule."+i, rules.get(i));
            }
        }
        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);
        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            log.info("Rcloud-更新防火墙{}成功", updateFirewallSO.getFwId());
            return UpdateFirewallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("Rcloud-更新防火墙{}失败：{}",updateFirewallSO.getFwId(),str);
            return UpdateFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 解绑主机的所有防火墙并清理无主防火墙：
     * 1.按ResourceId查该主机绑定的GroupId集合
     * 2.逐个解绑（DeleteSecurityGroup）
     * 3.解绑后查防火墙ResourceCount，为0说明没有其他主机在用，删除防火墙；否则保留
     * @param serviceNo 云平台实例ID
     * @return
     * @throws Exception
     */
    @Override
    public RevokeFirewallVO unbindAndCleanFirewalls(String serviceNo) throws Exception {
        Set<String> boundGroupIds = queryBoundGroupIds(serviceNo);
        if(boundGroupIds == null || boundGroupIds.isEmpty()){
            return RevokeFirewallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg("主机无绑定的防火墙")
                    .build();
        }
        for(String groupId : boundGroupIds){
            /** 解绑：DisassociateFirewall与GrantFirewall配对（LeaveSecurityGroup在部分平台不存在报161；DeleteSecurityGroup是删除安全组本体，挂资源时报4361） **/
            Map<String,String> param = new TreeMap<>();
            param.put("Action","DisassociateFirewall");
            param.put("PublicKey",pubKey);
            param.put("Region",regionId);
            if(StringUtils.isNotEmpty(projectId)){
                param.put("ProjectId",projectId);
            }
            param.put("FWId",groupId);// DisassociateFirewall需要FWId（firewall-xxx），传GroupId会报220 Missing params [FWId]
            param.put("ResourceType","uhost");
            param.put("ResourceId",serviceNo);
            String signature = getSignature(param,pivKey);
            param.put("Signature",signature);
            String str = HttpRequest.post(url,param);
            JSONObject json = JSONObject.fromObject(str);
            if(json.getInt("RetCode") == 0){
                log.info("Rcloud-主机{}解绑防火墙{}成功", serviceNo, groupId);
            }else{
                log.info("Rcloud-主机{}解绑防火墙{}失败：{}", serviceNo, groupId, str);
            }
        }
        /** 解绑后只检查刚解绑的这些防火墙：无其他主机在用(ResourceCount=0)的删除，否则保留 **/
        cleanIdleFirewalls(boundGroupIds);
        return RevokeFirewallVO.builder()
                .code(CommonUtil.SUCCESS_CODE)
                .msg("解绑完成")
                .build();
    }

    /**
     * 清理刚解绑的防火墙：逐条按GroupId精确查询（Rcloud的DescribeFirewall不支持FWId.N批量查询，会忽略参数返回全量），
     * 无其他主机在用(ResourceCount=0)的删除；仍有主机在用的保留并打日志给运维；其他防火墙一律不碰
     */
    private void cleanIdleFirewalls(Set<String> targetFwIds){
        if(targetFwIds == null || targetFwIds.isEmpty()){
            return;
        }
        for(String fwId : targetFwIds){
            try{
                String groupId = fwIdGroupIdMap.get(fwId);
                if(StringUtils.isEmpty(groupId)){
                    // 查不到GroupId就没法精确查询，跳过（宁可不删，不拉全量误判）
                    log.info("Rcloud-防火墙{}无GroupId映射，跳过清理", fwId);
                    continue;
                }
                Map<String,String> param = new TreeMap<>();
                param.put("Action","DescribeFirewall");
                param.put("PublicKey",pubKey);
                param.put("Region",regionId);
                if(StringUtils.isNotEmpty(projectId)){
                    param.put("ProjectId",projectId);
                }
                param.put("GroupId",groupId);
                String signature = getSignature(param,pivKey);
                param.put("Signature",signature);
                String str = HttpRequest.post(url,param);
                JSONObject json = JSONObject.fromObject(str);
                if(json.getInt("RetCode") != 0){
                    log.info("Rcloud-查询防火墙详情失败：{}",str);
                    continue;
                }
                JSONArray dataArray = json.optJSONArray("DataSet");
                if(dataArray == null || dataArray.size() == 0){
                    continue;
                }
                JSONObject data = dataArray.getJSONObject(0);
                int resourceCount = data.optInt("ResourceCount", -1);
                if(resourceCount == 0){
                    DeleteFirewallVO delVO = deleteFirewall(DeleteFirewallSO.builder().fwId(fwId).build());
                    if(delVO != null && CommonUtil.SUCCESS_CODE.equals(delVO.getCode())){
                        log.info("Rcloud-清理无绑定防火墙{}成功", fwId);
                    }
                }else{
                    log.info("Rcloud-防火墙{}仍有{}台主机在使用，已保留不删除", fwId, resourceCount);
                }
            }catch (Exception e){
                log.info("Rcloud-清理无绑定防火墙{}异常：{}", fwId, e.getMessage());
            }
        }
    }

    /**
     * 删除防火墙
     * @param deleteFirewallSO
     * @return
     * @throws Exception
     */
    @Override
    public DeleteFirewallVO deleteFirewall(DeleteFirewallSO deleteFirewallSO) throws Exception {
        Map<String,String> param = new TreeMap<>();
        param.put("Action","DeleteFirewall");
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("FWId",deleteFirewallSO.getFwId());
        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);
        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            log.info("Rcloud-删除防火墙{}成功", deleteFirewallSO.getFwId());
            return DeleteFirewallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("Rcloud-删除防火墙{}失败：{}",deleteFirewallSO.getFwId(),str);
            return DeleteFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
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
                .msg("Rcloud-不支持此功能")
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
                .msg("Rcloud-不支持此功能")
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
                .msg("Rcloud-不支持此功能")
                .build();
    }
    /**
     * 重装系统
     * @param reinstallSO
     * @return
     * @throws Exception
     */
    public ReinstallVO reinstall(ReinstallSO reinstallSO) throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","ReinstallUHostInstance");//重装系统
        param.put("PublicKey",pubKey);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//地域
        param.put("UHostId",reinstallSO.getInstanceId());//主机ID
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
            log.info("Rcloud-云主机{} 重装失败：{}",reinstallSO.getInstanceId(),str);
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
        /** UCloud要求主机必须先关机(SHUTOFF)才能销毁，否则报8204，这里先检查并关机 **/
        if(!waitUHostShutoff(destroySO.getInstanceId())){
            log.info("Rcloud-云主机{} 销毁失败：主机未能关机",destroySO.getInstanceId());
            return DestroyVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
        /** 关机后、销毁前解绑防火墙并清理无主防火墙：
         * 运行中解绑会报4361(security group is in use)；销毁后绑定关系随主机消失查不到，所以必须在关机后解绑 **/
        try{
            unbindAndCleanFirewalls(destroySO.getInstanceId());
        }catch (Exception e){
            log.info("Rcloud-主机{}销毁前防火墙解绑异常（不阻塞销毁）：{}",destroySO.getInstanceId(),e.getMessage());
        }
        Map<String,String> param = new TreeMap<>();
        param.put("Action","TerminateUHostInstance");
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("UHostId",destroySO.getInstanceId());
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            log.info("Rcloud-云主机{} 销毁成功", destroySO.getInstanceId());
            return DestroyVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }else{
            log.info("Rcloud-云主机{} 销毁参数：{}", destroySO.getInstanceId(), JSONObject.fromObject(param).toString());
            log.info("Rcloud-云主机{} 销毁失败：{}",destroySO.getInstanceId(),str);
            return DestroyVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 确保云主机关机(SHUTOFF)，供销毁前调用
     * 已关机直接返回true；否则发起关机并轮询等待，超时返回false
     */
    private boolean waitUHostShutoff(String instanceId)throws Exception{
        String state = getUHostState(instanceId);
        if("Stopped".equalsIgnoreCase(state)){
            return true;
        }
        if(state == null){
            return false;
        }
        /** 先关机 **/
        StopVO stopVO = stop(StopSO.builder().instanceId(instanceId).build());
        if(CommonUtil.FAIL_CODE == stopVO.getCode()){
            log.info("Rcloud-云主机{} 销毁前关机失败",instanceId);
            return false;
        }
        /** 轮询等待关机完成，最多等120秒 **/
        for(int i = 0; i < 24; i++){
            Thread.sleep(5000);
            state = getUHostState(instanceId);
            if("Stopped".equalsIgnoreCase(state)){
                return true;
            }
            /** null说明主机已查询不到(可能已被销毁)，放行 **/
            if(state == null){
                return true;
            }
        }
        log.info("Rcloud-云主机{} 等待关机超频，当前状态：{}",instanceId,state);
        return false;
    }

    /**
     * 查询单台云主机状态，查询不到返回null
     */
    private String getUHostState(String instanceId)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","DescribeUHostInstance");
        param.put("PublicKey",pubKey);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);
        param.put("UHostIds.0",instanceId);
        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);
        String str = HttpRequest.post(url,param);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") != 0){
            return null;
        }
        JSONArray instances = json.getJSONArray("UHostSet");
        if(instances == null || instances.isEmpty()){
            return null;
        }
        return JSONObject.fromObject(instances.get(0)).getString("State");
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
     * 创建E-IP
     * @param shareId 共享带宽ID
     * @param quantity 周期(月)
     * @return {"Action":"AllocateEIPResponse","EIPSet":[{"EIPAddr":[{"IP":"106.75.154.70","OperatorName":"BGP"}],"EIPId":"eip-ph86lmsf77y"}],"Request_uuid":"e3aee770-b733-48ff-b63c-108661ec33a5","RetCode":0}
     * @throws Exception
     */
    public Map<String,String> createEip(String shareId,Integer quantity)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","AllocateEIP");
        param.put("PublicKey",pubKey);

        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//地域
        param.put("OperatorName","Native");//国际线路:International,BGP线路:Bgp,精品BGP:BGPPro,原生IP：Native
        param.put("Bandwidth","0");//共享模式带宽必须填0
        param.put("ChargeType","Month");//Year, 按年付费; Month, 按月付费; Dynamic, 按时付费
        param.put("Quantity",quantity+"");
        param.put("ShareBandwidthId",shareId);
        param.put("PayMode","ShareBandwidth");//流量计费

        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);

        String str = HttpRequest.post(url,param);


        JSONObject result = JSONObject.fromObject(str);
        if(result.getInt("RetCode") == 0){
            JSONArray eipSet = result.getJSONArray("EIPSet");
            if(eipSet.size() > 0){
                JSONObject eip = eipSet.getJSONObject(0);
                Map<String,String> map = new HashMap<>();
                map.put("ip",eip.getJSONArray("EIPAddr").getJSONObject(0).getString("IP"));
                map.put("id",eip.getString("EIPId"));
                return map;
            }else{
                return null;
            }
        }else{
            log.info("Rcloud创建E-IP请求参数："+JSONObject.fromObject(param).toString());
            log.info("Rcloud创建E-IP响应结果："+str);
            return null;
        }
    }


    /**
     * IP绑定主机
     * @param eipId
     * @param instanceId
     * @throws Exception
     */
    public boolean bindIp(String eipId,String instanceId)throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","BindEIP");//绑定IP
        param.put("PublicKey",pubKey);

        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Region",regionId);//广州
        param.put("EIPId",eipId);//EIP-ID
        param.put("ResourceType","uhost");//绑定云主机类型
        param.put("ResourceId",instanceId);//云主机ID

        String signature = getSignature(param,pivKey);
        param.put("Signature",signature);


        String str = HttpRequest.post(url,param);
        JSONObject result = JSONObject.fromObject(str);
        if(result.getInt("RetCode") == 0){
            return true;
        }else{
            log.info("Rcloud IP绑定主机请求参数："+JSONObject.fromObject(param).toString());
            log.info("Rcloud IP绑定主机响应结果："+str);
            return false;
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
