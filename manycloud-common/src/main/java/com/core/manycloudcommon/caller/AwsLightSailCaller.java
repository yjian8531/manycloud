package com.core.manycloudcommon.caller;

import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.enums.PowerStateEnum;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.FileUtil;
import com.core.manycloudcommon.utils.JSchConnectUtils;
import com.core.manycloudcommon.utils.StringUtils;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;

import java.util.*;

/**
 * AWS 实例 Caller
 */
@Slf4j
public class AwsLightSailCaller implements BaseCaller{


    private static Map<String , AwsLightSailCaller> lightsailClientMap = new HashMap<>();

    private String regionId;

    private LightsailClient caller;

    private AwsLightSailCaller(LightsailClient caller){
        this.caller = caller;
    }

    /**
     * 获取AWS  客户端
     * @param accountApi
     * @return
     */
    public static AwsLightSailCaller getClient(AccountApi accountApi){
        if(lightsailClientMap.get(accountApi.getAccount()+":"+accountApi.getRegionId()) == null){
            synchronized(AwsLightSailCaller.class){
                LightsailClient lightsailClient = LightsailClient.builder().credentialsProvider(() -> new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return accountApi.getKeyNo();
                    }

                    @Override
                    public String secretAccessKey() {
                        return accountApi.getKeySecret();
                    }
                }).region(Region.of(accountApi.getRegionId()))
                        .build();
                AwsLightSailCaller caller = new AwsLightSailCaller(lightsailClient);
                caller.regionId = accountApi.getRegionId();
                lightsailClientMap.put(accountApi.getAccount()+":"+accountApi.getRegionId(),caller);
            }
            return lightsailClientMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }else{
            return lightsailClientMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }
    }


    public  List<Blueprint> getImage(){
        GetBlueprintsResponse response = caller.getBlueprints();
        return response.blueprints();
    }


    /**
     * 创建实例
     * @param createSO
     * @return
     * @throws Exception
     */
    @Override
    public CreateVO create(CreateSO createSO) throws Exception {
        List<String> instanceIds = new ArrayList<>();
        for(int i = 0 ; i < createSO.getNum() ; i++){
            try {
                String str = CommonUtil.getRandomStr(12);
                /** 先创建EIP **/
                String eipId = "eip-"+ str;
                AllocateStaticIpResponse response = caller.allocateStaticIp(AllocateStaticIpRequest.builder()
                        .staticIpName(eipId)
                        .build());


                /** 再创建实例 **/
                String instanceId = "aws-"+ str;
                String[] bz = createSO.getBundleId().split(":");
                String zone = bz[0];
                String bundle = bz[1];

                String keyName = CommonUtil.getRandomStr(10);
                CreateKeyPairRequest pairRequest = CreateKeyPairRequest.builder()
                        .keyPairName(keyName)
                        .build();
                CreateKeyPairResponse pairResponse = caller.createKeyPair(pairRequest);
                KeyPair keyPair = pairResponse.keyPair();
                FileUtil.saveFile(keyPair.name()+".pem",pairResponse.privateKeyBase64());

                caller.createInstances(CreateInstancesRequest.builder()
                        .instanceNames(instanceId)
                        .ipAddressType(IpAddressType.IPV4)
                        .keyPairName(keyPair.name())
                        .blueprintId(createSO.getImageId())
                        .availabilityZone(zone)
                        .bundleId(bundle)
                        .build());

                instanceIds.add(instanceId);
            }catch (Exception e){
                log.info("AWS LightSail 创建实例失败：{}",e.getMessage());
                e.printStackTrace();
            }
        }
        if(instanceIds.size() > 0){
            return CreateVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .instanceIds(instanceIds)
                    .build();
        }else{
            return CreateVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 实例查询创建
     * @param querySO
     * @return
     */
    @Override
    public QueryVO createQuery(QuerySO querySO){

        Map<String , QueryDetailVO> queryDetailMap = new HashMap<>();

        for(String instanceId : querySO.getInstanceIds()){

            try{

                /** 查询实例信息 **/
                Instance instance = caller.getInstance(GetInstanceRequest.builder()
                        .instanceName(instanceId)
                        .build()).instance();

                String state = instance.state().name().toLowerCase();
                String powerState;
                if("Starting".toLowerCase().equals(state) || "Upgrading".toLowerCase().equals(state)
                        || "Stopping".toLowerCase().equals(state) || "Resetting".toLowerCase().equals(state)){
                    /** 执行中 **/
                    powerState = PowerStateEnum.EXECUTION.getVal();
                }else if("Running".toLowerCase().equals(state.toLowerCase())){
                    /** 开机中 **/
                    powerState =  PowerStateEnum.RUNNING.getVal();
                }else if("Stopped".toLowerCase().equals(state.toLowerCase())){
                    /** 已关机 **/
                    powerState = PowerStateEnum.HALTED.getVal();
                }else{
                    /** 未知状态标签 **/
                    powerState = state;
                }

                if(16 == instance.state().code() && "running".equals(instance.state().name().toLowerCase())){

                    /** 查询IP信息 **/
                    String eipId = instanceId.replace("aws-","eip-");
                    GetStaticIpResponse response = caller.getStaticIp(GetStaticIpRequest.builder()
                            .staticIpName(eipId)
                            .build());
                    String eip = response.staticIp().ipAddress();

                    String privateIp = instance.privateIpAddress();
                    String publicIp = instance.publicIpAddress();
                    if(!eip.equals(publicIp)){
                        /** 绑定EIP信息 **/
                        caller.attachStaticIp(AttachStaticIpRequest.builder()
                                .instanceName(instanceId)
                                .staticIpName(eipId)
                                .build());
                    }

                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(instanceId)
                            .account(instance.username())
                            .port(22)
                            .publicIp(eip)
                            .privateIp(privateIp)
                            .status(1)
                            .pwd("https://www.lotvps.com:2205/file/"+instance.sshKeyName()+".pem")
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(instanceId,odvo);

                }else {
                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(instanceId)
                            .status(0)
                            .msg("AWS实例创建等待状态:"+state)
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(instanceId,odvo);
                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }

        return QueryVO.builder()
                .code(CommonUtil.SUCCESS_CODE)
                .msg(CommonUtil.SUCCESS_MSG)
                .queryDetailMap(queryDetailMap)
                .build();


    }


    /**
     * 实例查询创建
     * @param querySO
     * @return
     */
    @Override
    public QueryVO query(QuerySO querySO){

        Map<String , QueryDetailVO> queryDetailMap = new HashMap<>();

        for(String instanceId : querySO.getInstanceIds()){

            try{

                /** 查询实例信息 **/
                Instance instance = caller.getInstance(GetInstanceRequest.builder()
                        .instanceName(instanceId)
                        .build()).instance();

                String state = instance.state().name().toLowerCase();
                String powerState;
                if("Starting".toLowerCase().equals(state) || "Upgrading".toLowerCase().equals(state)
                        || "Stopping".toLowerCase().equals(state) || "Resetting".toLowerCase().equals(state)){
                    /** 执行中 **/
                    powerState = PowerStateEnum.EXECUTION.getVal();
                }else if("Running".toLowerCase().equals(state.toLowerCase())){
                    /** 开机中 **/
                    powerState =  PowerStateEnum.RUNNING.getVal();
                }else if("Stopped".toLowerCase().equals(state.toLowerCase())){
                    /** 已关机 **/
                    powerState = PowerStateEnum.HALTED.getVal();
                }else{
                    /** 未知状态标签 **/
                    powerState = state;
                }

                String privateIp = instance.privateIpAddress();
                String publicIp = instance.publicIpAddress();


                QueryDetailVO odvo = QueryDetailVO.builder()
                        .serviceNo(instanceId)
                        .account("root")
                        .port(22)
                        .publicIp(publicIp)
                        .privateIp(privateIp)
                        .status(1)
                        .powerState(powerState)
                        .build();
                queryDetailMap.put(instanceId,odvo);

            }catch (Exception e){
                e.printStackTrace();
            }

        }

        return QueryVO.builder()
                .code(CommonUtil.SUCCESS_CODE)
                .msg(CommonUtil.SUCCESS_MSG)
                .queryDetailMap(queryDetailMap)
                .build();


    }

    /**
     * 续费（没有到期时间-默认续费成功）
     * @param renewSO
     * @return
     */
    @Override
    public RenewVO renew(RenewSO renewSO){
        return RenewVO.builder()
                .code(CommonUtil.SUCCESS_CODE)
                .msg(CommonUtil.SUCCESS_MSG)
                .build();
    }

    @Override
    public StartVO start(StartSO startSO){
        try{
            caller.startInstance(StartInstanceRequest.builder()
                    .instanceName(startSO.getInstanceId())
                    .build());
            return StartVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("AWS主机[{}]-开机失败:{}",startSO.getInstanceId(),e.getMessage());
            return StartVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }

    }

    @Override
    public RebootVO reboot(RebootSO rebootSO){

        try{
            caller.rebootInstance(RebootInstanceRequest.builder()
                    .instanceName(rebootSO.getInstanceId())
                    .build());
            return RebootVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("AWS主机[{}]-重启失败:{}",rebootSO.getInstanceId(),e.getMessage());
            return RebootVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }

    }

    @Override
    public StopVO stop(StopSO stopSO){

        try{
            caller.stopInstance(StopInstanceRequest.builder()
                    .instanceName(stopSO.getInstanceId())
                    .build());
            return StopVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("AWS主机[{}]-关机失败:{}",stopSO.getInstanceId(),e.getMessage());
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
    @Override
    public ReinstallVO reinstall(ReinstallSO reinstallSO) throws Exception {
        return null;
    }

    /**
     * 销毁
     * @param destroySO
     * @return
     */
    @Override
    public DestroyVO destroy(DestroySO destroySO){

        /** 销毁实例 **/
        caller.deleteInstance(DeleteInstanceRequest.builder()
                .instanceName(destroySO.getInstanceId())
                .build());

        /** 查询IP信息 **/
        String eipId = destroySO.getInstanceId().replace("ws-","eip-");
        boolean bl = true;
        try{
            /** 查询EIP 验证其EIP是否存在 **/
            caller.getStaticIp(GetStaticIpRequest.builder().staticIpName(eipId).build());
        }catch (Exception e){
            bl = false;
        }

        if(bl){
            /** 销毁IP **/
            caller.releaseStaticIp(ReleaseStaticIpRequest.builder()
                    .staticIpName(eipId)
                    .build());
        }



        return null;
    }

    /**
     * 设置主机自动续费标识
     * @param updateAuteRenewSO
     * @return
     */
    @Override
    public UpdateAuteRenewVO updateAuteRenew(UpdateAuteRenewSO updateAuteRenewSO) throws Exception {
        return UpdateAuteRenewVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("AWS-不支持此功能")
                .build();
    }

    /**
     * 修改密码
     * @param updatePwdSO
     * @return
     * @throws Exception
     */
    @Override
    public UpdatePwdVO updatePwd(UpdatePwdSO updatePwdSO){

        try {

            DownloadDefaultKeyPairResponse keyPairResponse = caller.downloadDefaultKeyPair();
            Instance instance = caller.getInstance(GetInstanceRequest.builder()
                    .instanceName(updatePwdSO.getInstanceId())
                    .build()).instance();

            String pwd = updatePwdSO.getPwd();
            String keyName = instance.sshKeyName();
            // 创建JSch对象
            JSch jsch = new JSch();
            jsch.addIdentity(keyName, keyPairResponse.privateKeyBase64().getBytes(), keyPairResponse.publicKeyBase64().getBytes(), null);
            // 根据用户名，主机ip，端口获取一个Session对象
            Session session = jsch.getSession(instance.username(), instance.publicIpAddress(), 22);

            // 设置timeout时间
            session.setTimeout(60000000);
            // 为Session对象设置properties
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            // 通过Session建立链接
            session.connect();
            String str = "echo root:{pwd} |sudo chpasswd root";
            str = str.replace("{pwd}", pwd);
            String[] command = {
                    "#!/bin/bash",
                    str,
                    "sudo sed -i 's/PasswordAuthentication no/PasswordAuthentication yes/g' /etc/ssh/sshd_config",
                    "sudo systemctl restart sshd.service"
            };

            for (int i = 0; i < command.length; i++) {
                try {
                    log.info("AWS LightSail set pwd 执行命令:" + command[i]);
                    String s = JSchConnectUtils.execCommand(session, command[i]);
                    if (org.apache.commons.lang.StringUtils.isNotEmpty(s)) {
                        log.info("AWS LightSail set pwd 执行结果:" + str);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            session.disconnect();
            return UpdatePwdVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();

        }catch (Exception e){
            log.info("AWS LightSail 设置密码异常：{}",e.getMessage());
            e.printStackTrace();
            return UpdatePwdVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }



    }

    @Override
    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO) throws Exception {
        return CreateSecurityVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("AWS-不支持此功能")
                .build();
    }

    @Override
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception {
        return QueryFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("AWS-不支持此功能")
                .build();
    }

    @Override
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO) throws Exception {
        return GrantFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("AWS-不支持此功能")
                .build();
    }

    @Override
    public ClusterVO queryClusterList(ClusterListSO clusterListSO) throws Exception {
        return ClusterVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("AWS-不支持此功能")
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
                .msg("AWS-不支持此功能")
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
                .msg("AWS-不支持此功能")
                .build();
    }

}
