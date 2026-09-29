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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 腾讯云轻量应用服务器（Lighthouse）
 * 使用 HTTP + TC3-HMAC-SHA256 签名方式调用
 */
@Slf4j
public class TencentLighthouseCaller implements BaseCaller {

    private static Map<String, TencentLighthouseCaller> tencentLighthouseCallerMap = new HashMap<>();

    private static final String ENDPOINT = "lighthouse.tencentcloudapi.com";
    private static final String SERVICE = "lighthouse";
    private static final String VERSION = "2020-03-24";

    private String secretId;
    private String secretKey;
    private String region;

    private TencentLighthouseCaller(String secretId, String secretKey, String region) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.region = region;
    }

    /**
     * 获取客户端
     */
    public static TencentLighthouseCaller getClient(AccountApi accountApi) {
        String cacheKey = accountApi.getAccount() + ":" + accountApi.getRegionId();
        if (tencentLighthouseCallerMap.get(cacheKey) == null) {
            synchronized (TencentLighthouseCaller.class) {
                tencentLighthouseCallerMap.put(cacheKey, new TencentLighthouseCaller(
                        accountApi.getKeyNo(), accountApi.getKeySecret(), accountApi.getRegionId()));
            }
            return tencentLighthouseCallerMap.get(cacheKey);
        } else {
            return tencentLighthouseCallerMap.get(cacheKey);
        }
    }

    /**
     * 创建实例
     */
    @Override
    public CreateVO create(CreateSO createSO) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("BundleId", createSO.getBundleId());
        params.put("BlueprintId", createSO.getImageId());
        params.put("InstanceName", "MC-" + CommonUtil.getRandomStr(8));
        params.put("InstanceCount", 1);

        Map<String, Object> chargePrepaid = new HashMap<>();
        chargePrepaid.put("Period", createSO.getPeriod());
        chargePrepaid.put("RenewFlag", "NOTIFY_AND_MANUAL_RENEW");
        params.put("InstanceChargePrepaid", chargePrepaid);

        if (StringUtils.isNotEmpty(createSO.getZone())) {
            params.put("Zones", new String[]{createSO.getZone()});
        }

        try {
            String response = sendRequest("CreateInstances", JSONObject.fromObject(params).toString());
            log.info("腾讯云轻量-创建实例响应：{}", response);
            JSONObject resp = parseResponse(response);
            if (resp == null) {
                return CreateVO.builder().code(CommonUtil.FAIL_CODE).msg(CommonUtil.FAIL_MSG).build();
            }
            if (resp.has("InstanceIdSet")) {
                JSONArray instanceIdSet = resp.getJSONArray("InstanceIdSet");
                if (instanceIdSet.size() > 0) {
                    return CreateVO.builder()
                            .code(CommonUtil.SUCCESS_CODE)
                            .msg(CommonUtil.SUCCESS_MSG)
                            .instanceIds(Arrays.asList(instanceIdSet.getString(0)))
                            .build();
                }
            }
            log.info("腾讯云轻量-创建实例失败：未获取到实例ID，{}", response);
            return CreateVO.builder().code(CommonUtil.FAIL_CODE).msg(CommonUtil.FAIL_MSG).build();
        } catch (Exception e) {
            log.info("腾讯云轻量-创建实例异常：{}", e.getMessage());
            return CreateVO.builder().code(CommonUtil.FAIL_CODE).msg(CommonUtil.FAIL_MSG).build();
        }
    }

    /**
     * 查询主机创建信息（异步轮询）
     * RUNNING -> 成功(1)；LAUNCH_FAILED -> 失败(2)；其余 -> 等待(0)
     */
    @Override
    public QueryVO createQuery(QuerySO querySO) throws Exception {
        Map<String, QueryDetailVO> queryDetailMap = new HashMap<>();
        for (String instanceId : querySO.getInstanceIds()) {
            JSONObject instance = describeOneInstance(instanceId);
            if (instance == null) {
                queryDetailMap.put(instanceId, QueryDetailVO.builder()
                        .serviceNo(instanceId).status(0).msg("腾讯云轻量-查询实例异常").build());
                continue;
            }
            String state = instance.optString("InstanceState");
            String powerState = mapPowerState(state);

            QueryDetailVO.QueryDetailVOBuilder b = QueryDetailVO.builder()
                    .serviceNo(instanceId).powerState(powerState);

            if ("RUNNING".equalsIgnoreCase(state)) {
                fillInstanceDetail(b, instance);
                queryDetailMap.put(instanceId, b.status(1).build());
            } else if ("LAUNCH_FAILED".equalsIgnoreCase(state)) {
                queryDetailMap.put(instanceId, b.status(2).msg("腾讯云轻量-实例创建失败状态:" + state).build());
            } else {
                queryDetailMap.put(instanceId, b.status(0).msg("腾讯云轻量-实例创建等待状态:" + state).build());
            }
        }
        return QueryVO.builder()
                .code(CommonUtil.SUCCESS_CODE)
                .msg(CommonUtil.SUCCESS_MSG)
                .queryDetailMap(queryDetailMap)
                .build();
    }

    /**
     * 查询主机信息（常规）
     */
    @Override
    public QueryVO query(QuerySO querySO) throws Exception {
        Map<String, QueryDetailVO> queryDetailMap = new HashMap<>();
        for (String instanceId : querySO.getInstanceIds()) {
            JSONObject instance = describeOneInstance(instanceId);
            if (instance == null) {
                continue;
            }
            QueryDetailVO.QueryDetailVOBuilder b = QueryDetailVO.builder()
                    .serviceNo(instanceId)
                    .powerState(mapPowerState(instance.optString("InstanceState")));
            fillInstanceDetail(b, instance);
            queryDetailMap.put(instanceId, b.status(1).build());
        }
        return QueryVO.builder()
                .code(CommonUtil.SUCCESS_CODE)
                .msg(CommonUtil.SUCCESS_MSG)
                .queryDetailMap(queryDetailMap)
                .build();
    }

    /**
     * 续费
     */
    @Override
    public RenewVO renew(RenewSO renewSO) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("InstanceIds", new String[]{renewSO.getInstanceId()});
        Map<String, Object> chargePrepaid = new HashMap<>();
        chargePrepaid.put("Period", renewSO.getNum());
        chargePrepaid.put("RenewFlag", "NOTIFY_AND_MANUAL_RENEW");
        params.put("InstanceChargePrepaid", chargePrepaid);

        String requestJson = JSONObject.fromObject(params).toString();

        String response = sendRequest("RenewInstances", requestJson);
        log.info("腾讯云轻量-续费响应：{}", response);

        JSONObject resp = parseResponse(response);
        if (resp != null && resp.has("RequestId")) {
            log.info("腾讯云轻量-实例[{}]续费{}个月成功", renewSO.getInstanceId(), renewSO.getNum());
            return RenewVO.builder().code(CommonUtil.SUCCESS_CODE).msg(CommonUtil.SUCCESS_MSG).build();
        }
        String errMsg = extractError(response);
        log.info("腾讯云轻量-实例[{}]续费失败：{}", renewSO.getInstanceId(), errMsg);
        return RenewVO.builder().code(CommonUtil.FAIL_CODE).msg("腾讯云轻量-续费失败：" + errMsg).build();
    }

    /**
     * 从原始响应中提取腾讯错误信息（Code:Message），供失败时返回真实原因
     */
    private String extractError(String response) {
        try {
            if (StringUtils.isNotEmpty(response)) {
                JSONObject json = JSONObject.fromObject(response);
                JSONObject resp = json.optJSONObject("Response");
                JSONObject err = resp == null ? null : resp.optJSONObject("Error");
                if (err != null) {
                    return err.optString("Code") + ":" + err.optString("Message");
                }
            }
        } catch (Exception e) {
            log.info("腾讯云轻量-提取错误信息异常：{}", e.getMessage());
        }
        return StringUtils.isEmpty(response) ? "无响应" : response;
    }

    @Override
    public StartVO start(StartSO startSO) throws Exception {
        return simplePowerAction("StartInstances", startSO.getInstanceId())
                ? StartVO.builder().code(CommonUtil.SUCCESS_CODE).msg(CommonUtil.SUCCESS_MSG).build()
                : StartVO.builder().code(CommonUtil.FAIL_CODE).msg(CommonUtil.FAIL_MSG).build();
    }

    @Override
    public RebootVO reboot(RebootSO rebootSO) throws Exception {
        return simplePowerAction("RestartInstances", rebootSO.getInstanceId())
                ? RebootVO.builder().code(CommonUtil.SUCCESS_CODE).msg(CommonUtil.SUCCESS_MSG).build()
                : RebootVO.builder().code(CommonUtil.FAIL_CODE).msg(CommonUtil.FAIL_MSG).build();
    }

    @Override
    public StopVO stop(StopSO stopSO) throws Exception {
        return simplePowerAction("StopInstances", stopSO.getInstanceId())
                ? StopVO.builder().code(CommonUtil.SUCCESS_CODE).msg(CommonUtil.SUCCESS_MSG).build()
                : StopVO.builder().code(CommonUtil.FAIL_CODE).msg(CommonUtil.FAIL_MSG).build();
    }

    /**
     * 重装系统
     */
    @Override
    public ReinstallVO reinstall(ReinstallSO reinstallSO) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("InstanceId", reinstallSO.getInstanceId());
        if (StringUtils.isNotEmpty(reinstallSO.getImageId())) {
            params.put("BlueprintId", reinstallSO.getImageId());
        }
        if (StringUtils.isNotEmpty(reinstallSO.getPwd())) {
            params.put("Password", reinstallSO.getPwd());
        }
        return requestOk("ResetInstance", JSONObject.fromObject(params).toString())
                ? ReinstallVO.builder().code(CommonUtil.SUCCESS_CODE).msg(CommonUtil.SUCCESS_MSG).build()
                : ReinstallVO.builder().code(CommonUtil.FAIL_CODE).msg(CommonUtil.FAIL_MSG).build();
    }

    /**
     * 销毁（与liebao流程一致：RUNNING先关机 -> STOPPED退还进回收站 -> SHUTDOWN彻底销毁）
     */
    @Override
    public DestroyVO destroy(DestroySO destroySO) throws Exception {
        String instanceId = destroySO.getInstanceId();

        // 1. 查询实例当前状态
        JSONObject instance = describeOneInstance(instanceId);
        if (instance == null) {
            // 查询不到该主机，视为已销毁
            log.info("腾讯云轻量-查询不到该实例，视为已销毁 [{}]", instanceId);
            return DestroyVO.builder().code(CommonUtil.SUCCESS_CODE).msg(CommonUtil.SUCCESS_MSG).build();
        }
        String state = instance.optString("InstanceState");
        log.info("腾讯云轻量-销毁前实例状态 [{}]：{}", instanceId, state);

        Map<String, Object> params = new HashMap<>();
        params.put("InstanceIds", new String[]{instanceId});

        // 2. SHUTDOWN：此前已退还进回收站，直接彻底销毁
        if ("SHUTDOWN".equalsIgnoreCase(state)) {
            return terminateRecycled(instanceId, params);
        }

        // 3. RUNNING：先发送关机指令
        if ("RUNNING".equalsIgnoreCase(state)) {
            simplePowerAction("StopInstances", instanceId);
            log.info("腾讯云轻量-实例运行中，已发送关机指令 [{}]", instanceId);
        }

        // 4. 轮询等待关机完成（STOPPING等中间态），STOPPED才可退还；查询不到视为已销毁
        boolean stopped = false;
        for (int i = 0; i < 24; i++) {
            Thread.sleep(5000);
            JSONObject checkInstance = describeOneInstance(instanceId);
            if (checkInstance == null) {
                log.info("腾讯云轻量-查询不到该实例，视为已销毁 [{}]", instanceId);
                return DestroyVO.builder().code(CommonUtil.SUCCESS_CODE).msg(CommonUtil.SUCCESS_MSG).build();
            }
            if ("STOPPED".equalsIgnoreCase(checkInstance.optString("InstanceState"))) {
                stopped = true;
                break;
            }
        }
        if (!stopped) {
            log.info("腾讯云轻量-等待实例关机超时 [{}]", instanceId);
            return DestroyVO.builder().code(CommonUtil.FAIL_CODE).msg("等待实例关机超时").build();
        }

        // 5. 已关机：发送退还指令（进入回收站）
        if (!requestOk("IsolateInstances", JSONObject.fromObject(params).toString())) {
            log.info("腾讯云轻量-退还实例失败 [{}]", instanceId);
            return DestroyVO.builder().code(CommonUtil.FAIL_CODE).msg("退还实例失败").build();
        }
        log.info("腾讯云轻量-实例已关机，已发送退还指令 [{}]", instanceId);

        // 6. 轮询等待退还生效（SHUTDOWN），隔离后查询不到同样放行
        boolean recycled = false;
        for (int i = 0; i < 12; i++) {
            Thread.sleep(5000);
            JSONObject checkInstance = describeOneInstance(instanceId);
            if (checkInstance == null
                    || "SHUTDOWN".equalsIgnoreCase(checkInstance.optString("InstanceState"))) {
                recycled = true;
                break;
            }
        }
        if (!recycled) {
            log.info("腾讯云轻量-等待实例退还超时 [{}]", instanceId);
            return DestroyVO.builder().code(CommonUtil.FAIL_CODE).msg("等待实例退还超时").build();
        }

        // 7. 彻底销毁
        return terminateRecycled(instanceId, params);
    }

    /**
     * 从回收站彻底销毁实例
     */
    private DestroyVO terminateRecycled(String instanceId, Map<String, Object> params) throws Exception {
        if (!requestOk("TerminateInstances", JSONObject.fromObject(params).toString())) {
            log.info("腾讯云轻量-销毁失败 [{}]", instanceId);
            return DestroyVO.builder().code(CommonUtil.FAIL_CODE).msg(CommonUtil.FAIL_MSG).build();
        }
        log.info("腾讯云轻量-已发送销毁指令，销毁成功 [{}]", instanceId);
        return DestroyVO.builder().code(CommonUtil.SUCCESS_CODE).msg(CommonUtil.SUCCESS_MSG).build();
    }

    /**
     * 腾讯云轻量无直接修改自动续费标识的 API
     */
    @Override
    public UpdateAuteRenewVO updateAuteRenew(UpdateAuteRenewSO updateAuteRenewSO) throws Exception {
        return UpdateAuteRenewVO.builder().code(CommonUtil.FAIL_CODE).msg("腾讯云轻量-不支持此功能").build();
    }

    /**
     * 修改密码
     */
    @Override
    public UpdatePwdVO updatePwd(UpdatePwdSO updatePwdSO) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("InstanceIds", new String[]{updatePwdSO.getInstanceId()});
        params.put("Password", updatePwdSO.getPwd());
        return requestOk("ResetInstancesPassword", JSONObject.fromObject(params).toString())
                ? UpdatePwdVO.builder().code(CommonUtil.SUCCESS_CODE).msg(CommonUtil.SUCCESS_MSG).build()
                : UpdatePwdVO.builder().code(CommonUtil.FAIL_CODE).msg(CommonUtil.FAIL_MSG).build();
    }

    /**
     * 创建防火墙规则（按实例放行端口）
     * 腾讯CreateFirewallRules是整批追加且一票否决：批次里含任何已存在规则会整单报FirewallRulesExist。
     * 因此先查现有规则，过滤掉重复的只追加增量；全部已存在时直接返回成功（幂等，支持调用方重试）。
     */
    @Override
    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO) throws Exception {
        String instanceId = createSecuritySO.getInstanceId();
        if (StringUtils.isEmpty(instanceId)) {
            return CreateSecurityVO.builder().code(CommonUtil.FAIL_CODE).msg("腾讯云轻量-缺少实例ID").build();
        }

        // ① 查现有规则，建立"协议|端口|来源"键集合
        Set<String> existKeys = new HashSet<>();
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("InstanceId", instanceId);
        String queryResponse = sendRequest("DescribeFirewallRules", JSONObject.fromObject(queryParams).toString());
        JSONObject queryResp = parseResponse(queryResponse);
        if (queryResp != null && queryResp.has("FirewallRuleSet")) {
            JSONArray ruleSet = queryResp.getJSONArray("FirewallRuleSet");
            for (int i = 0; i < ruleSet.size(); i++) {
                JSONObject r = ruleSet.getJSONObject(i);
                existKeys.add(firewallRuleKey(r.optString("Protocol"), r.optString("Port"), r.optString("CidrBlock")));
            }
        }

        JSONArray rules = new JSONArray();

        // ② 组装候选规则，已存在（含本批次内重复）的跳过
        if (createSecuritySO.getRules() != null && !createSecuritySO.getRules().isEmpty()) {
            // 新接口格式：使用 rules 数组
            for (CreateSecuritySO.FirewallRule rule : createSecuritySO.getRules()) {
                String protocol = rule.getProtocol() != null ? rule.getProtocol() : "TCP";
                String cidr = rule.getSource() != null ? rule.getSource() : "0.0.0.0/0";
                String key = firewallRuleKey(protocol, rule.getPort(), cidr);
                if (existKeys.contains(key)) {
                    log.info("腾讯云轻量-规则已存在跳过：{} {}/{}", instanceId, protocol, rule.getPort());
                    continue;
                }
                existKeys.add(key);

                JSONObject tcp = new JSONObject();
                tcp.put("Protocol", protocol);
                tcp.put("Port", rule.getPort());
                tcp.put("CidrBlock", cidr);
                tcp.put("Action", "ACCEPT");
                tcp.put("FirewallRuleDescription", rule.getDescription() != null ? rule.getDescription() : "");
                rules.add(tcp);
            }
        } else if (StringUtils.isNotEmpty(createSecuritySO.getPort())) {
            // 旧接口格式：使用 port 字符串（逗号分隔），自动创建 TCP+UDP 规则
            for (String port : parsePorts(createSecuritySO.getPort())) {
                for (String protocol : new String[]{"TCP", "UDP"}) {
                    String key = firewallRuleKey(protocol, port, "0.0.0.0/0");
                    if (existKeys.contains(key)) {
                        log.info("腾讯云轻量-规则已存在跳过：{} {}/{}", instanceId, protocol, port);
                        continue;
                    }
                    existKeys.add(key);

                    JSONObject r = new JSONObject();
                    r.put("Protocol", protocol);
                    r.put("Port", port);
                    r.put("CidrBlock", "0.0.0.0/0");
                    r.put("Action", "ACCEPT");
                    r.put("FirewallRuleDescription", createSecuritySO.getName());
                    rules.add(r);
                }
            }
        } else {
            return CreateSecurityVO.builder().code(CommonUtil.FAIL_CODE).msg("腾讯云轻量-缺少防火墙规则").build();
        }

        // ③ 全部已存在：幂等返回成功，不再调云端
        if (rules.isEmpty()) {
            log.info("腾讯云轻量-防火墙规则全部已存在，无需追加：{}", instanceId);
            return CreateSecurityVO.builder()
                    .code(CommonUtil.SUCCESS_CODE).msg("防火墙规则已存在，无需重复添加").fwId(instanceId).build();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("InstanceId", instanceId);
        params.put("FirewallRules", rules);

        String requestJson = JSONObject.fromObject(params).toString();
        log.info("腾讯云轻量-创建防火墙规则请求：{}", requestJson);

        String response = sendRequest("CreateFirewallRules", requestJson);
        log.info("腾讯云轻量-创建防火墙规则响应：{}", response);
        JSONObject resp = parseResponse(response);
        if (resp != null && resp.has("RequestId")) {
            return CreateSecurityVO.builder()
                    .code(CommonUtil.SUCCESS_CODE).msg(CommonUtil.SUCCESS_MSG).fwId(instanceId).build();
        }
        log.info("腾讯云轻量-创建防火墙规则失败：{}", createSecuritySO.getFirewallName() != null ? createSecuritySO.getFirewallName() : createSecuritySO.getName());
        return null;
    }

    /** 防火墙规则唯一键（协议+端口+来源），协议统一大写 */
    private String firewallRuleKey(String protocol, String port, String cidr) {
        return (protocol == null ? "" : protocol.toUpperCase()) + "|" + (port == null ? "" : port) + "|" + (cidr == null ? "" : cidr);
    }

    /**
     * 查询防火墙规则
     */
    @Override
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception {
        String instanceId = StringUtils.isNotEmpty(queryFirewallSO.getFwId())
                ? queryFirewallSO.getFwId() : queryFirewallSO.getName();

        Map<String, Object> params = new HashMap<>();
        params.put("InstanceId", instanceId);
        String response = sendRequest("DescribeFirewallRules", JSONObject.fromObject(params).toString());
//        log.info("腾讯云轻量-查询防火墙规则响应：{}", response);
        JSONObject resp = parseResponse(response);

        List<FirewallRule> rules = new ArrayList<>();
        if (resp != null && resp.has("FirewallRuleSet")) {
            JSONArray ruleSet = resp.getJSONArray("FirewallRuleSet");
            for (int i = 0; i < ruleSet.size(); i++) {
                JSONObject r = ruleSet.getJSONObject(i);
                FirewallRule rule = new FirewallRule();
                rule.setProtocol(r.optString("Protocol"));
                rule.setPort(r.optString("Port"));
                rule.setAction(r.optString("Action"));
                rule.setIpAddress(r.optString("CidrBlock"));
                rule.setRemark(r.optString("FirewallRuleDescription"));
                rules.add(rule);
            }
        }
        return QueryFirewallVO.builder()
                .fwId(instanceId)
                .name(instanceId)
                .rules(rules)
                .code(CommonUtil.SUCCESS_CODE)
                .msg("查询成功")
                .build();
    }

    @Override
    public CreateFirewallTemplateRulesVO createFirewallTemplateRules(CreateFirewallTemplateRulesSO so) {
        return CreateFirewallTemplateRulesVO.builder()
                .code(CommonUtil.FAIL_CODE).msg("腾讯云轻量-不支持此功能").build();
    }

    /**
     * 腾讯云轻量防火墙规则直接绑定在实例上，无独立安全组绑定动作
     */
    @Override
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO) throws Exception {
        return GrantFirewallVO.builder()
                .success(false).code(CommonUtil.FAIL_CODE).msg("腾讯云轻量-不支持此功能").build();
    }

    /**
     * 腾讯云轻量暂不支持全量覆盖更新防火墙规则
     */
    @Override
    public UpdateFirewallVO updateFirewall(UpdateFirewallSO updateFirewallSO) throws Exception {
        return UpdateFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("腾讯云轻量-不支持此功能")
                .build();
    }

    /**
     * 腾讯云轻量防火墙规则直接绑定在实例上，无独立安全组可删除
     */
    @Override
    public DeleteFirewallVO deleteFirewall(DeleteFirewallSO deleteFirewallSO) throws Exception {
        return DeleteFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("腾讯云轻量-不支持此功能")
                .build();
    }

    /**
     * 腾讯云轻量防火墙规则直接绑定在实例上，无独立安全组可解绑；实例销毁时规则随之释放，无需清理
     */
    @Override
    public RevokeFirewallVO unbindAndCleanFirewalls(String serviceNo) throws Exception {
        return RevokeFirewallVO.builder()
                .code(CommonUtil.SUCCESS_CODE)
                .msg("腾讯云轻量-防火墙随实例销毁，无需解绑")
                .build();
    }

    @Override
    public ClusterVO queryClusterList(ClusterListSO clusterListSO) throws Exception {
        return ClusterVO.builder().code(CommonUtil.FAIL_CODE).msg("腾讯云轻量-不支持此功能").build();
    }

    @Override
    public TemplateListVO queryTemplateList(TemplateListSO templateListSO) throws Exception {
        return TemplateListVO.builder().code(CommonUtil.FAIL_CODE).msg("腾讯云轻量-不支持此功能").build();
    }

    @Override
    public PayVO orderPay(String orderId) throws Exception {
        return PayVO.builder().code(CommonUtil.FAIL_CODE).msg("腾讯云轻量-不支持此功能").build();
    }


    /* ========================= 以下为内部工具方法 ========================= */

    /**
     * 单实例电源操作（StartInstances / StopInstances / RestartInstances）
     */
    private boolean simplePowerAction(String action, String instanceId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("InstanceIds", new String[]{instanceId});
        return requestOk(action, JSONObject.fromObject(params).toString());
    }

    /**
     * 发送请求并仅判断是否成功（无 Error 即认为成功）
     */
    private boolean requestOk(String action, String requestBody) {
        try {
            String response = sendRequest(action, requestBody);
            JSONObject resp = parseResponse(response);
            return resp != null && resp.has("RequestId");
        } catch (Exception e) {
            log.info("腾讯云轻量-[{}]请求异常：{}", action, e.getMessage());
            return false;
        }
    }

    /**
     * 查询单个实例详情
     */
    private JSONObject describeOneInstance(String instanceId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("InstanceIds", new String[]{instanceId});
        String response = sendRequest("DescribeInstances", JSONObject.fromObject(params).toString());
        JSONObject resp = parseResponse(response);
        if (resp == null || !resp.has("InstanceSet")) {
            return null;
        }
        JSONArray instanceSet = resp.getJSONArray("InstanceSet");
        return instanceSet.size() > 0 ? instanceSet.getJSONObject(0) : null;
    }

    /**
     * 填充实例的 IP / 账号 / 端口
     */
    private void fillInstanceDetail(QueryDetailVO.QueryDetailVOBuilder b, JSONObject instance) {
        JSONArray publicIps = instance.optJSONArray("PublicAddresses");
        if (publicIps != null && publicIps.size() > 0) {
            b.publicIp(publicIps.getString(0));
        }
        JSONArray privateIps = instance.optJSONArray("PrivateAddresses");
        if (privateIps != null && privateIps.size() > 0) {
            b.privateIp(privateIps.getString(0));
        }
        String osType = instance.optString("OSType").toLowerCase();
        if ("windows".equals(osType)) {
            b.account("administrator").port(3389);
        } else {
            b.account("root").port(22);
        }
    }

    /**
     * 腾讯云实例状态 -> 系统统一电源状态
     */
    private String mapPowerState(String state) {
        if (StringUtils.isEmpty(state)) {
            return PowerStateEnum.EXECUTION.getVal();
        }
        String s = state.toLowerCase();
        if ("running".equals(s)) {
            return PowerStateEnum.RUNNING.getVal();
        } else if ("stopped".equals(s) || "shutdown".equals(s)) {
            return PowerStateEnum.HALTED.getVal();
        } else {
            // pending / starting / stopping / rebooting / moving / deleting 等
            return PowerStateEnum.EXECUTION.getVal();
        }
    }

    /**
     * 解析端口字符串（逗号分隔）
     */
    private List<String> parsePorts(String portStr) {
        List<String> ports = new ArrayList<>();
        if (StringUtils.isEmpty(portStr)) {
            return ports;
        }
        for (String p : portStr.split(",")) {
            String trimmed = p.trim();
            if (StringUtils.isNotEmpty(trimmed)) {
                ports.add(trimmed);
            }
        }
        return ports;
    }

    /**
     * 解析腾讯云响应，提取 Response 节点；含 Error 时记日志并返回 null
     */
    private JSONObject parseResponse(String response) {
        if (StringUtils.isEmpty(response)) {
            return null;
        }
        try {
            JSONObject json = JSONObject.fromObject(response);
            if (!json.has("Response")) {
                return null;
            }
            JSONObject resp = json.getJSONObject("Response");
            if (resp.has("Error")) {
                log.info("腾讯云轻量-响应错误：{}", resp.getJSONObject("Error"));
                return null;
            }
            return resp;
        } catch (Exception e) {
            log.info("腾讯云轻量-解析响应异常：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 发送请求（内部方法）
     */
    private String sendRequest(String action, String requestBody) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String date = getDate();

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Host", ENDPOINT);
        headers.put("X-TC-Action", action);
        headers.put("X-TC-Timestamp", timestamp);
        headers.put("X-TC-Version", VERSION);
        headers.put("X-TC-Region", region);
        headers.put("Authorization", sign(requestBody, timestamp, date));

        String url = "https://" + ENDPOINT;
        return HttpRequest.postJson(url, requestBody, headers);
    }

    /**
     * TC3-HMAC-SHA256 签名算法（腾讯云标准签名方式）
     */
    private String sign(String requestBody, String timestamp, String date) {
        try {
            String canonicalRequest = "POST\n/\n\ncontent-type:application/json; charset=utf-8\nhost:" + ENDPOINT + "\n\ncontent-type;host\n" + hash256(requestBody);

            String credentialScope = date + "/" + SERVICE + "/tc3_request";
            String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n" + hash256(canonicalRequest);

            byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
            byte[] secretService = hmac256(secretDate, SERVICE);
            byte[] secretSigning = hmac256(secretService, "tc3_request");
            String signature = bytesToHex(hmac256(secretSigning, stringToSign));

            return "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope
                    + ", SignedHeaders=content-type;host, Signature=" + signature;
        } catch (Exception e) {
            log.info("腾讯云轻量-签名失败：{}", e.getMessage());
            return "";
        }
    }

    private String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private String hash256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "";
        }
    }

    private byte[] hmac256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
