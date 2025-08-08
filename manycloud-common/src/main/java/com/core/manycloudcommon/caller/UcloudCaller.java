package com.core.manycloudcommon.caller;

import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.entity.FinanceDetail;
import com.core.manycloudcommon.entity.UserFinance;
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

    /**
     * 创建防火墙
     * @param port 端口
     * @return
     */
    public String createFirewall(String name,int port) throws Exception{
        Map<String,String> param = new TreeMap<>();
        param.put("Action","CreateFirewall");
        param.put("PublicKey",pubKey);
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Name",name);
        param.put("Rule.0","TCP|22|0.0.0.0/0|ACCEPT|HIGH|开的TCP22端口");
        param.put("Rule.1","UDP|22|0.0.0.0/0|ACCEPT|HIGH|开的UDP22端口");
        param.put("Rule.2","TCP|"+port+"|0.0.0.0/0|ACCEPT|HIGH|开的TCP"+port+"端口");
        param.put("Rule.3","UDP|"+port+"|0.0.0.0/0|ACCEPT|HIGH|开的UDP"+port+"端口");
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        //log.info("请求参数："+ JSONObject.fromObject(param).toString());
        String str = HttpRequest.post(url,param);
        //log.info("响应结果："+str);
        JSONObject json = JSONObject.fromObject(str);
        if(json.getInt("RetCode") == 0){
            return json.getString("FWId");
        }else{
            log.info("ucloud-创建防火墙{}失败：{}",name,str);
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
        param.put("Region",regionId);
        if(StringUtils.isNotEmpty(projectId)){
            param.put("ProjectId",projectId);
        }
        param.put("Limit","10000");
        if(StringUtils.isNotEmpty(fwId)){
            param.put("FWId",fwId);
        }
        String signature =  getSignature(param,pivKey);
        param.put("Signature",signature);
        //log.info("请求参数："+ JSONObject.fromObject(param).toString());
        String str = HttpRequest.post(url,param);
        //log.info("响应结果："+str);

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
                }else{
                    if(name.equals(data.getString("Name"))){
                        groupId = data.getString("GroupId");
                        break;
                    }
                }

            }
        }
        return groupId;
    }

    /**
     * 关联防火墙安全组信息
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
            log.info("ucloud-关联防火墙安全组信息："+str);
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
