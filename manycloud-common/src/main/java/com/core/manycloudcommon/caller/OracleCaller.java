package com.core.manycloudcommon.caller;

import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.entity.FirewallRule;
import com.core.manycloudcommon.enums.PowerStateEnum;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.JSchConnectUtils;
import com.core.manycloudcommon.utils.StringUtils;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.oracle.bmc.Realm;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.StringPrivateKeySupplier;
import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.VirtualNetworkClient;
import com.oracle.bmc.core.model.*;
import com.oracle.bmc.core.requests.*;
import com.oracle.bmc.core.responses.*;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Oracle云（甲骨文 OCI）平台调用器
 * 按需计费无续费概念；创建时不支持设密码（固定公钥+延续任务SSH改root密）；
 * 防火墙为NSG(Network Security Group)独立实体，绑定在网卡(VNIC)上
 */
@Slf4j
public class OracleCaller implements BaseCaller{

    private static Map<String , OracleCaller> oracleCallerMap = new HashMap<>();

    private String region;

    private String compartmentId;

    private AuthenticationDetailsProvider adp;

    /** vcnId缓存（同账号同区域只有一个VCN，创建NSG时用） **/
    private String vcnId;

    public static OracleCaller getClient(AccountApi accountApi){
        if(oracleCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId()) == null){
            synchronized(OracleCaller.class){
                OracleCaller oracleCaller = new OracleCaller(accountApi.getRegionId(),accountApi.getAccount(),accountApi.getKeyNo());
                oracleCallerMap.put(accountApi.getAccount()+":"+accountApi.getRegionId(),oracleCaller);
            }
            return oracleCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }else{
            return oracleCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }
    }

    public OracleCaller(String region, String account, String keyNo){
        JSONObject json = CommonUtil.getOracleAccount(account);
        this.region = region;
        this.compartmentId = json.getString("compartmentId");
        StringPrivateKeySupplier privateKeySupplier = new StringPrivateKeySupplier(CommonUtil.getOraclePrivateKey(keyNo));

        Region regionEntity = null;
        try{
            regionEntity = Region.fromRegionId(region);
        }catch (Exception e){
            log.info(e.getMessage());
        }
        if(regionEntity == null){
            regionEntity = Region.register(region, Realm.OC1);
        }

        AuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
                .userId(json.getString("userId"))
                .tenantId(json.getString("tenantId"))
                .fingerprint(json.getString("fingerprint"))
                .region(regionEntity)
                .privateKeySupplier(privateKeySupplier).build();
        this.adp = provider;
    }


    /**
     * 创建实例（异步，OCID立即返回，状态PROVISIONING→RUNNING）
     * @param createSO imageId=镜像OCID；zone=可用域AD/子网OCID(用/分隔)；
     *                 bundleId=机型shape(如VM.Standard.E4.Flex)；cpu/ram=ocpus/内存GB
     */
    @Override
    public CreateVO create(CreateSO createSO){
        try{
            ComputeClient client = ComputeClient.builder().region(region).build(adp);
            InstanceSourceViaImageDetails sourceViaImageDetails = InstanceSourceViaImageDetails.builder()
                    .imageId(createSO.getImageId())
                    .build();

            /** 创建时不能设密码，注入固定公钥，配合延续任务SSH改root密 **/
            Map<String, String> metadataMap = new HashMap<>();
            metadataMap.put("ssh_authorized_keys", SSH_AUTHORIZED_KEY);

            String[] array = createSO.getZone().split("/");
            LaunchInstanceDetails launchInstanceDetails = LaunchInstanceDetails.builder()
                    .compartmentId(compartmentId)
                    .subnetId(array[1])//子网ID(ocid1.subnet.oc1.xxx)
                    .shape(createSO.getBundleId())//机型(如VM.Standard.E4.Flex)
                    .availabilityDomain(array[0])//可用域(ZbZG:AP-SINGAPORE-1-AD-1)
                    .sourceDetails(sourceViaImageDetails)
                    .shapeConfig(LaunchInstanceShapeConfigDetails.builder()
                            .ocpus(Float.parseFloat(createSO.getCpu()))
                            .memoryInGBs(Float.parseFloat(createSO.getRam()))
                            .baselineOcpuUtilization(LaunchInstanceShapeConfigDetails.BaselineOcpuUtilization.Baseline18)
                            .build())
                    .metadata(metadataMap)
                    .build();

            LaunchInstanceResponse response = client.launchInstance(LaunchInstanceRequest.builder()
                    .launchInstanceDetails(launchInstanceDetails).build());

            if(response.getInstance() == null || response.getInstance().getId() == null){
                log.info("Oracle-创建实例失败，参数:{}", createSO);
                return CreateVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg("创建失败：响应无实例ID")
                        .build();
            }
            log.info("Oracle-创建实例结果:{}", response.getInstance());
            return CreateVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .instanceIds(Arrays.asList(response.getInstance().getId()))
                    .build();
        }catch (Exception e){
            log.info("Oracle-创建实例异常:{}", e.getMessage());
            e.printStackTrace();
            return CreateVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 查询主机创建信息：RUNNING后查网卡回填公网IP
     * @param querySO
     */
    @Override
    public QueryVO createQuery(QuerySO querySO){
        return queryInstanceInfo(querySO.getInstanceIds().get(0));
    }

    /**
     * 查询实例信息
     * @param querySO
     * @return
     */
    @Override
    public QueryVO query(QuerySO querySO){
        return queryInstanceInfo(querySO.getInstanceIds().get(0));
    }

    /**
     * 续费：Oracle按需计费无到期时间，恒成功
     */
    @Override
    public RenewVO renew(RenewSO renewSO){
        return RenewVO.builder()
                .code(CommonUtil.SUCCESS_CODE)
                .msg("Oracle按需计费，无需续费")
                .build();
    }

    /**
     * 开机（STOPPED状态实例，Oracle实例动作API）
     */
    @Override
    public StartVO start(StartSO startSO){
        try{
            doInstanceAction(startSO.getInstanceId(), "START", "开机");
            return StartVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            return StartVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 关机（硬关机，等效拔电源）
     */
    @Override
    public StopVO stop(StopSO stopSO){
        try{
            doInstanceAction(stopSO.getInstanceId(), "STOP", "关机");
            return StopVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            return StopVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 重启（软重启）
     */
    @Override
    public RebootVO reboot(RebootSO rebootSO){
        try{
            doInstanceAction(rebootSO.getInstanceId(), "SOFTRESET", "重启");
            return RebootVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            return RebootVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 重装系统：Oracle不支持rebuild，需销毁后重新创建
     */
    @Override
    public ReinstallVO reinstall(ReinstallSO reinstallSO){
        return ReinstallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("Oracle-不支持重装，请销毁后重新创建")
                .build();
    }

    /**
     * 销毁（TerminateInstance异步终止，连同引导卷一起删除）
     * 销毁前先解绑安全组并清理无主NSG（不阻塞销毁），与Rcloud销毁时序一致
     */
    @Override
    public DestroyVO destroy(DestroySO destroySO){
        String instanceId = destroySO.getInstanceId();
        try{
            /** 销毁前解绑NSG：实例存活时才能查到网卡绑定关系，解绑后无主的NSG顺手清理 **/
            try{
                unbindAndCleanFirewalls(instanceId);
            }catch (Exception e){
                log.info("Oracle-实例{}销毁前安全组解绑异常（不阻塞销毁）：{}", instanceId, e.getMessage());
            }
            ComputeClient client = ComputeClient.builder().build(adp);
            client.terminateInstance(TerminateInstanceRequest.builder()
                    .instanceId(instanceId).build());
            return DestroyVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("Oracle-实例[{}]销毁异常:{}", instanceId, e.getMessage());
            e.printStackTrace();
            return DestroyVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 设置自动续费标识：按需计费无此概念，恒成功
     */
    @Override
    public UpdateAuteRenewVO updateAuteRenew(UpdateAuteRenewSO updateAuteRenewSO){
        return UpdateAuteRenewVO.builder()
                .code(CommonUtil.SUCCESS_CODE)
                .msg("Oracle按需计费，无需自动续费")
                .build();
    }

    /**
     * 更新密码：Oracle不支持API改密
     */
    @Override
    public UpdatePwdVO updatePwd(UpdatePwdSO updatePwdSO){
        return UpdatePwdVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("Oracle-不支持API修改密码")
                .build();
    }

    /**
     * 创建安全组（NSG）：规则=出站全放+入站DROP只放行SSH 22+上游指定端口
     * @param createSecuritySO name=NSG名称；port/rules=放行端口
     */
    @Override
    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO){
        String name = createSecuritySO.getName();
        try{
            VirtualNetworkClient client = VirtualNetworkClient.builder().build(adp);

            /** 创建NSG **/
            CreateNetworkSecurityGroupResponse response = client.createNetworkSecurityGroup(
                    CreateNetworkSecurityGroupRequest.builder()
                            .createNetworkSecurityGroupDetails(CreateNetworkSecurityGroupDetails.builder()
                                    .compartmentId(compartmentId)
                                    .displayName(name)
                                    .vcnId(getVcnId()).build())
                            .build());
            String groupId = response.getNetworkSecurityGroup().getId();
            log.info("Oracle-创建安全组[{}]成功：{}", name, groupId);

            /** 添加规则：出站全放 + 入站22/指定端口 **/
            client.addNetworkSecurityGroupSecurityRules(AddNetworkSecurityGroupSecurityRulesRequest.builder()
                    .networkSecurityGroupId(groupId)
                    .addNetworkSecurityGroupSecurityRulesDetails(buildRulesDetails(createSecuritySO))
                    .build());
            log.info("Oracle-安全组[{}]规则添加成功（出站全放+入站放行端口）", groupId);

            return CreateSecurityVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .fwId(groupId)
                    .build();
        }catch (Exception e){
            log.info("Oracle-创建安全组[{}]异常:{}", name, e.getMessage());
            e.printStackTrace();
            return CreateSecurityVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 查询安全组：按名displayName / 按fwId(NSG OCID或实例OCID) / 按实例绑定关系 三层兜底
     */
    @Override
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO){
        try{
            JSONObject nsgJson = null;
            //1、按名称查
            if(StringUtils.isNotEmpty(queryFirewallSO.getName())){
                nsgJson = findNsgByName(queryFirewallSO.getName());
            }
            //2、按fwId查（可能是NSG OCID，也可能是业务约定传入的实例OCID）
            if(nsgJson == null && StringUtils.isNotEmpty(queryFirewallSO.getFwId())){
                String fwId = queryFirewallSO.getFwId();
                if(fwId.startsWith("ocid1.instance.")){
                    nsgJson = firstBoundNsg(fwId);
                }else{
                    nsgJson = getNsgById(fwId);
                    if(nsgJson == null){
                        nsgJson = firstBoundNsg(fwId);
                    }
                }
            }
            //3、按实例ID查绑定的NSG
            if(nsgJson == null && StringUtils.isNotEmpty(queryFirewallSO.getInstanceId())){
                nsgJson = firstBoundNsg(queryFirewallSO.getInstanceId());
            }

            if(nsgJson == null){
                /** 没查到：返回成功但fwId为空，业务层据此走新建流程 **/
                log.info("Oracle-安全组按名/编号/实例绑定均未查到，走新建流程");
                return QueryFirewallVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg("查询成功")
                        .build();
            }
            log.info("Oracle-查到安全组:{}", nsgJson.optString("id"));
            return parseNsgData(nsgJson);
        }catch (Exception e){
            log.info("Oracle-查安全组异常:{}", e.getMessage());
            e.printStackTrace();
            return QueryFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 阿里云安全组模板：Oracle不支持
     */
    @Override
    public CreateFirewallTemplateRulesVO createFirewallTemplateRules(CreateFirewallTemplateRulesSO so) {
        return CreateFirewallTemplateRulesVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("Oracle-不支持此功能")
                .build();
    }

    /**
     * 绑定安全组：把NSG挂到实例的网卡(VNIC)上，已绑定则直接成功（幂等）
     */
    @Override
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO){
        String groupId = grantFirewallSO.getGroupId();
        String instanceId = grantFirewallSO.getInstanceId();
        try{
            List<VnicAttachment> vnicList = queryVnic(instanceId);
            if(vnicList == null || vnicList.isEmpty()){
                return GrantFirewallVO.builder()
                        .success(false)
                        .code(CommonUtil.FAIL_CODE)
                        .msg("实例无网卡，无法绑定")
                        .build();
            }
            VirtualNetworkClient client = VirtualNetworkClient.builder().build(adp);
            for(VnicAttachment va : vnicList){
                String vnicId = va.getVnicId();
                Vnic vnic = queryIp(vnicId);
                List<String> nsgIds = vnic.getNsgIds() == null ? new ArrayList<>() : new ArrayList<>(vnic.getNsgIds());
                if(!nsgIds.contains(groupId)){
                    nsgIds.add(groupId);
                    client.updateVnic(UpdateVnicRequest.builder()
                            .vnicId(vnicId)
                            .updateVnicDetails(UpdateVnicDetails.builder()
                                    .nsgIds(nsgIds)
                                    .skipSourceDestCheck(true).build())
                            .build());
                    log.info("Oracle-安全组[{}]绑定网卡[{}]成功", groupId, vnicId);
                }else{
                    log.info("Oracle-安全组[{}]已绑定网卡[{}]，跳过", groupId, vnicId);
                }
            }
            return GrantFirewallVO.builder()
                    .success(true)
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("Oracle-绑定安全组[{}->{}]异常:{}", groupId, instanceId, e.getMessage());
            e.printStackTrace();
            return GrantFirewallVO.builder()
                    .success(false)
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 更新安全组规则：Oracle为删旧加新（先清空全部规则再写入：出站全放+入站22/指定端口）
     * @param updateFirewallSO fwId=NSG OCID；rules=协议|端口|源IP|动作|优先级|备注
     */
    @Override
    public UpdateFirewallVO updateFirewall(UpdateFirewallSO updateFirewallSO){
        String groupId = updateFirewallSO.getFwId();
        try{
            VirtualNetworkClient client = VirtualNetworkClient.builder().build(adp);

            /** 删除全部旧规则 **/
            removeNsgAllRules(client, groupId);

            /** 写入新规则（固定放行SSH 22 + 上游传入规则） **/
            CreateSecuritySO createSecuritySO = new CreateSecuritySO();
            createSecuritySO.setPort(null);
            List<CreateSecuritySO.FirewallRule> ruleList = new ArrayList<>();
            if(updateFirewallSO.getRules() != null){
                for(String line : updateFirewallSO.getRules()){
                    if(StringUtils.isEmpty(line)){
                        continue;
                    }
                    String[] parts = line.split("\\|");
                    if(parts.length < 3){
                        continue;
                    }
                    String protocol = parts[0].trim().toUpperCase();
                    if(!"TCP".equals(protocol) && !"UDP".equals(protocol) && !"ICMP".equals(protocol) && !"ALL".equals(protocol)){
                        protocol = "TCP";
                    }
                    CreateSecuritySO.FirewallRule rule = new CreateSecuritySO.FirewallRule();
                    rule.setProtocol(protocol);
                    rule.setPort(parts[1].trim());
                    rule.setSource(parts.length > 2 ? parts[2].trim() : "0.0.0.0/0");
                    ruleList.add(rule);
                }
            }
            createSecuritySO.setRules(ruleList);
            client.addNetworkSecurityGroupSecurityRules(AddNetworkSecurityGroupSecurityRulesRequest.builder()
                    .networkSecurityGroupId(groupId)
                    .addNetworkSecurityGroupSecurityRulesDetails(buildRulesDetails(createSecuritySO))
                    .build());

            return UpdateFirewallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("Oracle-更新安全组[{}]规则异常:{}", groupId, e.getMessage());
            e.printStackTrace();
            return UpdateFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 删除安全组（有VNIC绑定时Oracle会拒绝删除，需先解绑）
     */
    @Override
    public DeleteFirewallVO deleteFirewall(DeleteFirewallSO deleteFirewallSO){
        String groupId = deleteFirewallSO.getFwId();
        try{
            VirtualNetworkClient client = VirtualNetworkClient.builder().build(adp);
            client.deleteNetworkSecurityGroup(DeleteNetworkSecurityGroupRequest.builder()
                    .networkSecurityGroupId(groupId).build());
            return DeleteFirewallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("Oracle-删除安全组[{}]异常:{}", groupId, e.getMessage());
            e.printStackTrace();
            return DeleteFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 解绑主机的所有安全组并清理无主NSG：
     * 清空实例所有网卡的NSG绑定，解绑后无任何VNIC绑定的NSG直接删除
     * @param serviceNo 云平台实例OCID
     */
    @Override
    public RevokeFirewallVO unbindAndCleanFirewalls(String serviceNo){
        try{
            List<VnicAttachment> vnicList;
            try{
                vnicList = queryVnic(serviceNo);
            }catch (Exception e){
                /** 实例已不存在（如已销毁），无绑定可解 **/
                log.info("Oracle-实例[{}]查询网卡失败，视为无需解绑:{}", serviceNo, e.getMessage());
                return RevokeFirewallVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg("实例不存在，无需解绑")
                        .build();
            }
            if(vnicList == null || vnicList.isEmpty()){
                return RevokeFirewallVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }
            VirtualNetworkClient client = VirtualNetworkClient.builder().build(adp);
            Set<String> boundNsgIds = new HashSet<>();
            for(VnicAttachment va : vnicList){
                String vnicId = va.getVnicId();
                Vnic vnic = queryIp(vnicId);
                if(vnic.getNsgIds() != null && !vnic.getNsgIds().isEmpty()){
                    boundNsgIds.addAll(vnic.getNsgIds());
                }
                /** 清空该网卡的全部NSG绑定 **/
                client.updateVnic(UpdateVnicRequest.builder()
                        .vnicId(vnicId)
                        .updateVnicDetails(UpdateVnicDetails.builder()
                                .nsgIds(new ArrayList<>())
                                .skipSourceDestCheck(true).build())
                        .build());
                log.info("Oracle-网卡[{}]解绑全部安全组成功", vnicId);
            }
            /** 清理无主NSG（无任何VNIC绑定才删，多机共用的保留） **/
            for(String nsgId : boundNsgIds){
                ListNetworkSecurityGroupVnicsResponse vnicResponse = client.listNetworkSecurityGroupVnics(
                        ListNetworkSecurityGroupVnicsRequest.builder()
                                .networkSecurityGroupId(nsgId).build());
                if(vnicResponse.getItems() == null || vnicResponse.getItems().isEmpty()){
                    try{
                        client.deleteNetworkSecurityGroup(DeleteNetworkSecurityGroupRequest.builder()
                                .networkSecurityGroupId(nsgId).build());
                        log.info("Oracle-清理无主安全组[{}]成功", nsgId);
                    }catch (Exception e){
                        log.info("Oracle-清理无主安全组[{}]失败:{}", nsgId, e.getMessage());
                    }
                }
            }
            return RevokeFirewallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("Oracle-解绑清理实例[{}]安全组异常:{}", serviceNo, e.getMessage());
            e.printStackTrace();
            return RevokeFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 查询集群列表：Oracle不支持
     */
    @Override
    public ClusterVO queryClusterList(ClusterListSO clusterListSO) {
        return ClusterVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("Oracle-不支持此功能")
                .build();
    }

    /**
     * 查询模板列表：Oracle不支持
     */
    @Override
    public TemplateListVO queryTemplateList(TemplateListSO templateListSO) {
        return TemplateListVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("Oracle-不支持此功能")
                .build();
    }

    /**
     * 订单支付：Oracle不支持
     */
    @Override
    public PayVO orderPay(String orderId) {
        return PayVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("Oracle-不支持此功能")
                .build();
    }

    // ============================== 防火墙/实例辅助方法 ==============================

    /** 创建实例时注入的固定SSH公钥（与resources/file/ssh-key-oracle.key私钥配对，延续任务用私钥登录改root密码） **/
    private static final String SSH_AUTHORIZED_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCmoOD3MuuUBRO9ibY/x4SgzEzQV8O8qkGBv4ojN4pYorZEFLbUqc9LzTFKk4Xx9WZaB8SVykw2BMYNh5q6aIpYcajCe1mLApFZCXtnlIPLD/32O7I7SzjATr5+YbDgbfoV9ZAdvibeuWTQpmbSV0+k5mq183cgw2ndh4z3JcLj+r6g6dlTOa8WuhR+jrehvAlBpw9YmsnBFW6R2Iqvpj8w2gwoeFsxa7zEStxkuylUMDxoyjRMh3nYLkWwrS8cVzopcyhEqqkftTaoTcyoqFko5YWzWjKbiNZB6678kxaHURysfsne8nb1URBbt75b2YvR+ttvkowPOi1XXbZ2cOxN ssh-key-2024-05-07";

    /**
     * 实例动作（开机/关机/重启），Oracle实例动作API，失败抛异常
     * @param action START/STOP/SOFTRESET/RESET/SOFTSTOP
     */
    private void doInstanceAction(String instanceId, String action, String actionName){
        try{
            ComputeClient client = ComputeClient.builder().build(adp);
            client.instanceAction(InstanceActionRequest.builder()
                    .instanceId(instanceId)
                    .action(action)
                    .build());
        }catch (Exception e){
            log.info("Oracle-实例[{}]{}异常:{}", instanceId, actionName, e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Oracle-" + actionName + "失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询实例信息（创建轮询与通用查询共用）：RUNNING后查网卡回填公网IP
     */
    private QueryVO queryInstanceInfo(String instanceId){
        try{
            ComputeClient client = ComputeClient.builder().region(region).build(adp);
            Instance instance = client.getInstance(GetInstanceRequest.builder()
                    .instanceId(instanceId).build()).getInstance();
            if(instance == null){
                return QueryVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg("实例不存在")
                        .build();
            }
            String state = instance.getLifecycleState().getValue().toUpperCase();
            /**
             * PROVISIONING STARTING RUNNING STOPPING STOPPED TERMINATING TERMINATED
             * **/
            String powerState;
            if("PROVISIONING".equals(state) || "STARTING".equals(state)
                    || "STOPPING".equals(state) || "RESUMING".equals(state)){
                /** 执行中 **/
                powerState = PowerStateEnum.EXECUTION.getVal();
            }else if("RUNNING".equals(state)){
                powerState = PowerStateEnum.RUNNING.getVal();
            }else if("STOPPED".equals(state)){
                powerState = PowerStateEnum.HALTED.getVal();
            }else{
                /** 未知状态标签 **/
                powerState = state;
            }

            Map<String , QueryDetailVO> queryDetailMap = new HashMap<>();

            if("RUNNING".equals(state)){//主机状态运行中
                /** 查网卡拿公网IP（RUNNING后VNIC可能还没就绪，未就绪按等待处理下轮再查） **/
                String publicIp = null;
                String privateIp = null;
                List<VnicAttachment> vnicList = queryVnic(instanceId);
                for(VnicAttachment va : vnicList){
                    Vnic vnic = queryIp(va.getVnicId());
                    if(vnic.getPublicIp() != null && !vnic.getPublicIp().isEmpty()){
                        publicIp = vnic.getPublicIp();
                        privateIp = vnic.getPrivateIp();
                        break;
                    }
                }
                if(StringUtils.isEmpty(publicIp)){
                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(instanceId)
                            .status(0)
                            .msg("Oracle实例创建等待状态:VNIC未就绪")
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(instanceId,odvo);
                }else{
                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(instanceId)
                            .account("root")
                            .port(22)
                            .publicIp(publicIp)
                            .privateIp(privateIp == null ? "" : privateIp)
                            .status(1)
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(instanceId,odvo);
                }
            }else if("TERMINATED".equals(state) || "TERMINATING".equals(state)){
                /** 已销毁/销毁中：创建轮询场景按失败处理 **/
                QueryDetailVO odvo = QueryDetailVO.builder()
                        .serviceNo(instanceId)
                        .status(2)
                        .msg("Oracle实例已销毁:"+state)
                        .powerState(powerState)
                        .build();
                queryDetailMap.put(instanceId,odvo);
            }else{
                /** 其他状态：等待 **/
                QueryDetailVO odvo = QueryDetailVO.builder()
                        .serviceNo(instanceId)
                        .status(0)
                        .msg("Oracle实例创建等待状态:"+state)
                        .powerState(powerState)
                        .build();
                queryDetailMap.put(instanceId,odvo);
            }

            return QueryVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .queryDetailMap(queryDetailMap)
                    .build();
        }catch (Exception e){
            log.info("Oracle-查询实例[{}]异常:{}", instanceId, e.getMessage());
            e.printStackTrace();
            return QueryVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 查询实例的网卡列表
     */
    public List<VnicAttachment> queryVnic(String instanceId){
        ComputeClient client = ComputeClient.builder().region(region).build(adp);
        ListVnicAttachmentsResponse response = client.listVnicAttachments(ListVnicAttachmentsRequest.builder()
                .compartmentId(compartmentId)
                .instanceId(instanceId)
                .build());
        return response.getItems();
    }

    /**
     * 根据网卡OCID查询网卡详情（公网IP/私网IP/绑定的NSG列表）
     */
    public Vnic queryIp(String vnicId){
        VirtualNetworkClient client = VirtualNetworkClient.builder().build(adp);
        return client.getVnic(GetVnicRequest.builder().vnicId(vnicId).build()).getVnic();
    }

    /**
     * SSH用固定私钥登录实例修改root密码并开启密码登录（延续任务用，与liebao的OracleTime.setupRootAccount一致）
     * @param host 公网IP
     * @param pwd 新root密码
     * @return 是否全部命令执行成功
     */
    public boolean setRootPwdBySsh(String host, String pwd){
        boolean cmdFailed = false;
        Session session = null;
        try{
            InputStream inputStream = OracleCaller.class.getClassLoader().getResourceAsStream("file/ssh-key-oracle.key");
            Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
            byte[] bytes = scanner.hasNext() ? scanner.next().getBytes("UTF-8") : new byte[0];
            scanner.close();

            JSch jsch = new JSch();
            jsch.addIdentity("ssh-key-oracle", bytes, null, null);
            session = jsch.getSession("opc", host, 22);
            session.setTimeout(60000000);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            String str = "echo root:{pwd} |sudo chpasswd root";
            str = str.replace("{pwd}", pwd);
            String[] command = {
                    "#!/bin/bash",
                    str,
                    "sudo sed -i 's/PasswordAuthentication no/PasswordAuthentication yes/g' /etc/ssh/sshd_config",
                    "sudo systemctl restart sshd.service"
            };
            for(String cmd : command){
                try{
                    log.info("Oracle-SSH改root密 执行命令:{}", cmd);
                    String s = JSchConnectUtils.execCommand(session, cmd);
                    log.info("Oracle-SSH改root密 执行结果:{}", s);
                }catch (Exception e){
                    //命令执行失败不能静默当成功，否则密码实际未改成但任务被标记完成
                    log.error("Oracle-SSH改root密 命令执行失败：{}", e.getMessage(), e);
                    cmdFailed = true;
                }
            }
        }catch (Exception e){
            log.error("Oracle-SSH连接[{}]失败:{}", host, e.getMessage(), e);
            return false;
        }finally {
            if(session != null && session.isConnected()){
                session.disconnect();
            }
        }
        return !cmdFailed;
    }

    /**
     * 获取VCN ID（同compartment取第一个，创建NSG用；结果缓存）
     */
    private String getVcnId(){
        if(StringUtils.isNotEmpty(vcnId)){
            return vcnId;
        }
        VirtualNetworkClient client = VirtualNetworkClient.builder().build(adp);
        ListVcnsResponse lvr = client.listVcns(ListVcnsRequest.builder()
                .compartmentId(compartmentId).build());
        for(Vcn vcn : lvr.getItems()){
            vcnId = vcn.getId();
            break;
        }
        if(StringUtils.isEmpty(vcnId)){
            throw new RuntimeException("Oracle-compartment下未找到VCN");
        }
        return vcnId;
    }

    /**
     * 构建NSG规则集：出站全放 + 入站DROP只放行SSH 22+上游指定端口
     */
    private AddNetworkSecurityGroupSecurityRulesDetails buildRulesDetails(CreateSecuritySO createSecuritySO){
        Set<String> seen = new HashSet<>();
        List<AddSecurityRuleDetails> rules = new ArrayList<>();
        /** 出站全放（Oracle NSG无默认放行，必须显式加） **/
        rules.add(AddSecurityRuleDetails.builder()
                .direction(AddSecurityRuleDetails.Direction.Egress)
                .destinationType(AddSecurityRuleDetails.DestinationType.CidrBlock)
                .destination("0.0.0.0/0")
                .source("0.0.0.0/0")
                .protocol("all")
                .isStateless(false)
                .build());
        /** 入站：固定放行SSH 22 **/
        if(seen.add("TCP:22")){
            rules.add(buildIngressRule("TCP", "22", "0.0.0.0/0"));
        }
        /** 入站：上游指定端口 **/
        List<CreateSecuritySO.FirewallRule> rulesList = createSecuritySO.getRules();
        if(rulesList != null && !rulesList.isEmpty()){
            for(CreateSecuritySO.FirewallRule rule : rulesList){
                String protocol = StringUtils.isNotEmpty(rule.getProtocol()) ? rule.getProtocol().trim().toUpperCase() : "TCP";
                if("ALL".equals(protocol)){
                    addIngressRule(rules, seen, "TCP", rule.getPort(), rule.getSource());
                    addIngressRule(rules, seen, "UDP", rule.getPort(), rule.getSource());
                }else{
                    addIngressRule(rules, seen, protocol, rule.getPort(), rule.getSource());
                }
            }
        }else{
            /** 旧格式：port字符串，默认TCP+UDP双协议（与Rcloud一致） **/
            if(StringUtils.isNotEmpty(createSecuritySO.getPort())){
                for(String port : createSecuritySO.getPort().split(",")){
                    String item = port.trim();
                    if(StringUtils.isEmpty(item)){
                        continue;
                    }
                    addIngressRule(rules, seen, "TCP", item, "0.0.0.0/0");
                    addIngressRule(rules, seen, "UDP", item, "0.0.0.0/0");
                }
            }
        }
        return AddNetworkSecurityGroupSecurityRulesDetails.builder()
                .securityRules(rules).build();
    }

    /**
     * 追加入站放行规则（ICMP无端口；按协议+端口去重）
     */
    private void addIngressRule(List<AddSecurityRuleDetails> rules, Set<String> seen, String protocol, String port, String source){
        if("ICMP".equals(protocol)){
            if(seen.add("ICMP:")){
                rules.add(AddSecurityRuleDetails.builder()
                        .direction(AddSecurityRuleDetails.Direction.Ingress)
                        .sourceType(AddSecurityRuleDetails.SourceType.CidrBlock)
                        .source(StringUtils.isNotEmpty(source) ? source.trim() : "0.0.0.0/0")
                        .protocol("1")
                        .isStateless(false)
                        .build());
            }
            return;
        }
        if(StringUtils.isEmpty(port)){
            return;
        }
        String portTrim = port.trim();
        if(seen.add(protocol + ":" + portTrim)){
            rules.add(buildIngressRule(protocol, portTrim, source));
        }
    }

    /**
     * 构建单条入站规则（TCP协议6/UDP协议17，端口支持"80"或"8080-8090"范围）
     */
    private AddSecurityRuleDetails buildIngressRule(String protocol, String port, String source){
        int min;
        int max;
        if(port.contains("-")){
            String[] range = port.split("-");
            min = Integer.parseInt(range[0].trim());
            max = Integer.parseInt(range[1].trim());
        }else{
            min = Integer.parseInt(port);
            max = min;
        }
        String cidr = StringUtils.isNotEmpty(source) ? source.trim() : "0.0.0.0/0";
        AddSecurityRuleDetails.Builder builder = AddSecurityRuleDetails.builder()
                .direction(AddSecurityRuleDetails.Direction.Ingress)
                .sourceType(AddSecurityRuleDetails.SourceType.CidrBlock)
                .source(cidr)
                .isStateless(false);
        if("UDP".equals(protocol)){
            builder.protocol("17")
                    .udpOptions(UdpOptions.builder()
                            .destinationPortRange(PortRange.builder().min(min).max(max).build()).build());
        }else{
            builder.protocol("6")
                    .tcpOptions(TcpOptions.builder()
                            .destinationPortRange(PortRange.builder().min(min).max(max).build()).build());
        }
        return builder.build();
    }

    /**
     * 按名称查NSG（displayName精确匹配，分页查全量），没找到返回null
     */
    private JSONObject findNsgByName(String name){
        VirtualNetworkClient client = VirtualNetworkClient.builder().build(adp);
        List<NetworkSecurityGroup> allNsg = new ArrayList<>();
        String nextPageToken = null;
        do{
            ListNetworkSecurityGroupsResponse listResponse = client.listNetworkSecurityGroups(
                    ListNetworkSecurityGroupsRequest.builder()
                            .compartmentId(compartmentId)
                            .page(nextPageToken)
                            .limit(100)
                            .build());
            if(listResponse.getItems() != null){
                allNsg.addAll(listResponse.getItems());
            }
            nextPageToken = listResponse.getOpcNextPage();
        }while (nextPageToken != null && !nextPageToken.isEmpty());

        for(NetworkSecurityGroup nsg : allNsg){
            if(name.equals(nsg.getDisplayName())){
                JSONObject json = new JSONObject();
                json.put("id", nsg.getId());
                json.put("displayName", nsg.getDisplayName());
                return json;
            }
        }
        return null;
    }

    /**
     * 按NSG OCID查，没找到返回null
     */
    private JSONObject getNsgById(String nsgId){
        try{
            VirtualNetworkClient client = VirtualNetworkClient.builder().build(adp);
            NetworkSecurityGroup nsg = client.getNetworkSecurityGroup(GetNetworkSecurityGroupRequest.builder()
                    .networkSecurityGroupId(nsgId).build()).getNetworkSecurityGroup();
            JSONObject json = new JSONObject();
            json.put("id", nsg.getId());
            json.put("displayName", nsg.getDisplayName());
            return json;
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 查实例绑定的第一个NSG（业务约定：fwId可能承载实例OCID），没找到返回null
     */
    private JSONObject firstBoundNsg(String instanceId){
        try{
            List<VnicAttachment> vnicList = queryVnic(instanceId);
            for(VnicAttachment va : vnicList){
                Vnic vnic = queryIp(va.getVnicId());
                if(vnic.getNsgIds() != null && !vnic.getNsgIds().isEmpty()){
                    return getNsgById(vnic.getNsgIds().get(0));
                }
            }
        }catch (Exception e){
            log.info("Oracle-查实例[{}]绑定NSG失败:{}", instanceId, e.getMessage());
        }
        return null;
    }

    /**
     * NSG转QueryFirewallVO：groupId与fwId同值（Oracle无组概念），规则转FirewallRule列表
     */
    private QueryFirewallVO parseNsgData(JSONObject nsgJson){
        String fwId = nsgJson.getString("id");
        QueryFirewallVO vo = QueryFirewallVO.builder()
                .fwId(fwId)
                .groupId(fwId)
                .name(nsgJson.getString("displayName"))
                .rules(new ArrayList<>())
                .code(CommonUtil.SUCCESS_CODE)
                .msg("查询成功")
                .build();
        try{
            VirtualNetworkClient client = VirtualNetworkClient.builder().build(adp);
            ListNetworkSecurityGroupSecurityRulesResponse rulesResponse = client.listNetworkSecurityGroupSecurityRules(
                    ListNetworkSecurityGroupSecurityRulesRequest.builder()
                            .networkSecurityGroupId(fwId).build());
            if(rulesResponse.getItems() != null){
                for(SecurityRule rule : rulesResponse.getItems()){
                    FirewallRule entity = new FirewallRule();
                    entity.setFirewallId(fwId);
                    String protocol = rule.getProtocol();
                    if("6".equals(protocol)){
                        entity.setProtocol("TCP");
                    }else if("17".equals(protocol)){
                        entity.setProtocol("UDP");
                    }else if("1".equals(protocol)){
                        entity.setProtocol("ICMP");
                    }else{
                        entity.setProtocol(protocol);
                    }
                    /** 端口取目标端口范围 **/
                    String port = null;
                    if(rule.getTcpOptions() != null && rule.getTcpOptions().getDestinationPortRange() != null){
                        PortRange range = rule.getTcpOptions().getDestinationPortRange();
                        port = range.getMin().equals(range.getMax()) ? String.valueOf(range.getMin()) : range.getMin() + "-" + range.getMax();
                    }else if(rule.getUdpOptions() != null && rule.getUdpOptions().getDestinationPortRange() != null){
                        PortRange range = rule.getUdpOptions().getDestinationPortRange();
                        port = range.getMin().equals(range.getMax()) ? String.valueOf(range.getMin()) : range.getMin() + "-" + range.getMax();
                    }
                    entity.setPort(port);
                    /** 入站取源IP，出站取目标IP **/
                    if(rule.getDirection() == SecurityRule.Direction.Ingress){
                        entity.setIpAddress(rule.getSource());
                        entity.setDirection("inbound");
                    }else{
                        entity.setIpAddress(rule.getDestination());
                        entity.setDirection("outbound");
                    }
                    /** Oracle规则只放行无拒绝动作 **/
                    entity.setAction("ACCEPT");
                    vo.getRules().add(entity);
                }
            }
        }catch (Exception e){
            log.info("Oracle-查安全组[{}]规则失败:{}", fwId, e.getMessage());
        }
        return vo;
    }

    /**
     * 删除NSG的全部规则（更新规则=删旧加新）
     */
    private void removeNsgAllRules(VirtualNetworkClient client, String groupId){
        ListNetworkSecurityGroupSecurityRulesResponse rulesResponse = client.listNetworkSecurityGroupSecurityRules(
                ListNetworkSecurityGroupSecurityRulesRequest.builder()
                        .networkSecurityGroupId(groupId).build());
        if(rulesResponse.getItems() == null || rulesResponse.getItems().isEmpty()){
            return;
        }
        List<String> removeRuleIds = new ArrayList<>();
        for(SecurityRule rule : rulesResponse.getItems()){
            removeRuleIds.add(rule.getId());
        }
        client.removeNetworkSecurityGroupSecurityRules(RemoveNetworkSecurityGroupSecurityRulesRequest.builder()
                .networkSecurityGroupId(groupId)
                .removeNetworkSecurityGroupSecurityRulesDetails(RemoveNetworkSecurityGroupSecurityRulesDetails.builder()
                        .securityRuleIds(removeRuleIds).build())
                .build());
    }
}
