package com.core.manycloudcommon.caller;

import com.aliyun.teaopenapi.Client;
import com.aliyun.teaopenapi.models.OpenApiRequest;
import com.aliyun.teaopenapi.models.Params;
import com.aliyun.teautil.models.RuntimeOptions;
import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.enums.PowerStateEnum;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

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
            log.info("ucloud-创建轻量级云主机失败：{}",e.getMessage());
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

    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO) throws Exception {
        return CreateSecurityVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿里云-不支持此功能")
                .build();
    }

    @Override
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception {
        return QueryFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿里云-不支持此功能")
                .build();
    }

    @Override
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO) throws Exception {
        return GrantFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿里云-不支持此功能")
                .build();
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



}
