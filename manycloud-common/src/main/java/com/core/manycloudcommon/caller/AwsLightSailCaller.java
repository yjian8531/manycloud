package com.core.manycloudcommon.caller;

import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.entity.FirewallRule;
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
     * 查询AWS实例快照列表（AWS Lightsail的自定义镜像就是实例快照）
     * @return 实例快照列表
     */
    public List<InstanceSnapshot> getInstanceSnapshots(){
        try {
            GetInstanceSnapshotsResponse response = caller.getInstanceSnapshots();
            return response.instanceSnapshots();
        } catch (Exception e) {
            log.error("AWS LightSail 查询实例快照失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询套餐信息
     * @return 套餐列表
     */
    public List<Bundle> getBundles(){
        GetBundlesResponse response = caller.getBundles();
        return response.bundles();
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

        // 判断是否为特殊用户（通过awsSpecialFlag标识）
        boolean isSpecialUser = (createSO.getAwsSpecialFlag() != null);

        for(int i = 0 ; i < createSO.getNum() ; i++){
            try {
                String str = CommonUtil.getRandomStr(12);
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

                // 特殊用户：直接创建实例，不创建EIP，支持快照创建
                if (isSpecialUser) {
                    // 特殊用户使用快照创建实例，需要用专门的API
                    String[] bundleParts = bundle.split("_");
                    String bundleSize = bundleParts[0]; // nano, micro等
                    String bundleBundleId = bundle; // 完整bundle ID

                    caller.createInstancesFromSnapshot(CreateInstancesFromSnapshotRequest.builder()
                            .instanceNames(instanceId)
                            .instanceSnapshotName(createSO.getImageId())  // 使用快照名称
                            .bundleId(bundleBundleId)
                            .availabilityZone(zone)
                            .keyPairName(keyPair.name())
                            .build());
                    log.info("AWS LightSail 特殊用户创建实例（跳过EIP）: {}", instanceId);
                }
                // 普通用户：先创建EIP，再创建实例
                else {
                    // 先创建EIP
                    String eipId = "eip-"+ str;
                    AllocateStaticIpResponse response = caller.allocateStaticIp(AllocateStaticIpRequest.builder()
                            .staticIpName(eipId)
                            .build());

                    // 再创建实例
                    caller.createInstances(CreateInstancesRequest.builder()
                            .instanceNames(instanceId)
                            .ipAddressType(IpAddressType.IPV4)
                            .keyPairName(keyPair.name())
                            .blueprintId(createSO.getImageId())
                            .availabilityZone(zone)
                            .bundleId(bundle)
                            .build());
                    log.info("AWS LightSail 普通用户创建实例（含EIP）: {}", instanceId);
                }

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

                    /** 查询IP信息 - 区分普通用户和特殊用户 **/
                    String eipId = instanceId.replace("aws-","eip-");
                    String privateIp = instance.privateIpAddress();
                    String finalPublicIp;

                    /** 查询EIP列表，判断是否有对应的EIP **/
                    GetStaticIpsResponse eipsResponse = caller.getStaticIps(GetStaticIpsRequest.builder().build());
                    boolean hasEip = eipsResponse.staticIps().stream()
                            .anyMatch(eip -> eip.name().equals(eipId));

                    if (hasEip) {
                        /** 普通用户：有EIP，使用EIP **/
                        GetStaticIpResponse response = caller.getStaticIp(GetStaticIpRequest.builder()
                                .staticIpName(eipId)
                                .build());
                        String eip = response.staticIp().ipAddress();

                        String publicIp = instance.publicIpAddress();
                        if(!eip.equals(publicIp)){
                            /** 绑定EIP信息 **/
                            caller.attachStaticIp(AttachStaticIpRequest.builder()
                                    .instanceName(instanceId)
                                    .staticIpName(eipId)
                                    .build());
                        }
                        finalPublicIp = eip;
                        log.info("AWS普通用户使用EIP: instanceId={}, eip={}", instanceId, finalPublicIp);

                    } else {
                        /** 特殊用户：无EIP，使用实例自带IP **/
                        finalPublicIp = instance.publicIpAddress();
                        log.info("AWS特殊用户使用实例自带IP: instanceId={}, publicIp={}", instanceId, finalPublicIp);
                    }

                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(instanceId)
                            .account(instance.username())
                            .port(22)
                            .publicIp(finalPublicIp)
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
     * 重装系统 (AWS Lightsail 不支持该功能)
     * @param reinstallSO
     * @return
     * @throws Exception
     */
    @Override
    public ReinstallVO reinstall(ReinstallSO reinstallSO) throws Exception {
        log.info("AWS LightSail 不支持重装系统功能");
        return ReinstallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("AWS LightSail 平台不支持重装系统功能")
                .build();

    }

    /**
     * 销毁
     * @param destroySO
     * @return
     */
    @Override
    public DestroyVO destroy(DestroySO destroySO){

        try {
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

            return DestroyVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();

        } catch (Exception e) {
            log.info("AWS主机[{}]销毁失败:{}", destroySO.getInstanceId(), e.getMessage());
            return DestroyVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
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
        try {
            String instanceName = createSecuritySO.getInstanceId();
            List<PortInfo> portInfos = new ArrayList<>();

            // 添加默认端口：SSH (22)
            portInfos.add(PortInfo.builder()
                    .protocol("tcp")
                    .fromPort(22)
                    .toPort(22)
                    .cidrs(Collections.singletonList("0.0.0.0/0"))
                    .build());

            // 如果用户指定了端口，添加用户指定的TCP端口
            if (createSecuritySO.getPort() != null && !createSecuritySO.getPort().isEmpty()) {
                try {
                    int port = Integer.parseInt(createSecuritySO.getPort());
                    portInfos.add(PortInfo.builder()
                            .protocol("tcp")
                            .fromPort(port)
                            .toPort(port)
                            .cidrs(Collections.singletonList("0.0.0.0/0"))
                            .build());
                } catch (NumberFormatException e) {
                    log.warn("AWS LightSail 端口格式错误，忽略用户端口配置");
                }
            }

            // 如果有防火墙规则列表，处理规则
            if (createSecuritySO.getRules() != null && !createSecuritySO.getRules().isEmpty()) {
                for (CreateSecuritySO.FirewallRule rule : createSecuritySO.getRules()) {
                    if ("tcp".equalsIgnoreCase(rule.getProtocol()) || "udp".equalsIgnoreCase(rule.getProtocol())) {
                        try {
                            String[] ports = rule.getPort().split("-");
                            int fromPort = Integer.parseInt(ports[0]);
                            int toPort = ports.length > 1 ? Integer.parseInt(ports[1]) : fromPort;

                            portInfos.add(PortInfo.builder()
                                    .protocol(rule.getProtocol())
                                    .fromPort(fromPort)
                                    .toPort(toPort)
                                    .cidrs(Collections.singletonList(rule.getSource()))
                                    .build());
                        } catch (NumberFormatException e) {
                            log.warn("AWS LightSail 端口规则格式错误: {}", rule.getPort());
                        }
                    }
                }
            }

            // 调用AWS API配置端口
            if (!portInfos.isEmpty()) {
                PutInstancePublicPortsRequest portsRequest = PutInstancePublicPortsRequest.builder()
                        .instanceName(instanceName)
                        .portInfos(portInfos.toArray(new PortInfo[0]))
                        .build();

                caller.putInstancePublicPorts(portsRequest);

                return CreateSecurityVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .fwId(instanceName)
                        .build();
            } else {
                return CreateSecurityVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg("没有有效的端口配置")
                        .build();
            }

        } catch (Exception e) {
            log.error("AWS LightSail 创建安全组失败: {}", e.getMessage(), e);
            return CreateSecurityVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg("创建安全组失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception {
        try {
            // 优先使用fwId字段（包含真实的云平台实例ID），其次使用instanceId
            String instanceName = StringUtils.isNotEmpty(queryFirewallSO.getFwId())
                    ? queryFirewallSO.getFwId()
                    : queryFirewallSO.getInstanceId();

            // 使用AWS Lightsail的getInstancePortStates API查询端口状态
            GetInstancePortStatesResponse response = caller.getInstancePortStates(
                    GetInstancePortStatesRequest.builder()
                            .instanceName(instanceName)
                            .build());

            // 转换端口信息为FirewallRule列表
            List<FirewallRule> rules = new ArrayList<>();
            for (InstancePortState portState : response.portStates()) {
                FirewallRule rule = new FirewallRule();
                rule.setFirewallId(instanceName);
                rule.setProtocol(portState.protocol().toString());
                //修改端口显示
                String portRange = portState.fromPort().equals(portState.toPort()) ? String.valueOf(portState.fromPort()) : portState.fromPort() + "-" + portState.toPort();
                rule.setPort(portRange);
                rule.setIpAddress(portState.cidrs() != null && !portState.cidrs().isEmpty() ?
                        portState.cidrs().get(0) : "0.0.0.0/0");
                rule.setAction("accept");
                rules.add(rule);
            }

            // AWS Lightsail使用实例名作为groupId
            return QueryFirewallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .groupId(instanceName) // AWS Lightsail使用实例名作为groupId
                    .fwId(instanceName)
                    .name(instanceName)
                    .rules(rules)
                    .build();

        } catch (Exception e) {
            log.error("AWS LightSail 查询安全组失败: {}", e.getMessage(), e);
            return QueryFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg("查询安全组失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public CreateFirewallTemplateRulesVO createFirewallTemplateRules(CreateFirewallTemplateRulesSO so) {
        return CreateFirewallTemplateRulesVO.builder()
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
