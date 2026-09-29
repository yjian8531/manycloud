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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class AkamaiCaller implements BaseCaller{

    private static Map<String , AkamaiCaller> akamaiCallerMap = new HashMap<>();

    private static final String BASE_URL = "https://api.linode.com/v4";

    private static final String FIREWALL_URL = BASE_URL + "/networking/firewalls";

    private String token;
    private String account;
    private String region;





    public static AkamaiCaller getClient(AccountApi accountApi){
        if(akamaiCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId()) == null){
            synchronized(AkamaiCaller.class){
                AkamaiCaller akamaiCaller = new AkamaiCaller(accountApi.getRegionId(),accountApi.getAccount(),accountApi.getKeyNo());
                akamaiCallerMap.put(accountApi.getAccount()+":"+accountApi.getRegionId(),akamaiCaller);
            }
            return akamaiCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }else{
            return akamaiCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }
    }

    private AkamaiCaller(String region, String account, String apiKey){
        this.region = region;
        this.account = account;
        this.token = apiKey;
    }

    public String getImage(){
        String url = "https://api.linode.com/v4/images?page=1&page_size=100";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        try {
            String result = HttpRequest.get(url,headerMap);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 创建实例
     * @param createSO
     * @throws Exception
     */
    public CreateVO create(CreateSO createSO){
        String url = "https://api.linode.com/v4/linode/instances";

        String instanceName = "akm-"+ CommonUtil.getRandomStr(12);
        Map<String,Object> param = new HashMap<>();
        param.put("region",region);//地域（us-sea 美国-华盛顿）
        param.put("type",createSO.getBundleId());//规格类型(g6-nanode-1)
        param.put("label",instanceName);//标签
        param.put("image",createSO.getImageId());//镜像ID(linode/centos7)
        param.put("root_pass",createSO.getPwd());//密码

        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        try{
            String result = HttpRequest.postJson(url, JSONObject.fromObject(param).toString(),headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("id") == null || StringUtils.isEmpty(json.getString("id"))){
                log.info("阿卡麦-创建实例参数:{}",JSONObject.fromObject(param).toString());
                log.info("阿卡麦-创建实例结果:{}",result);
                return CreateVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }else{
                String instanceId = json.getString("id");
                log.info("阿卡麦-创建实例结果:{}", result);
                return CreateVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .instanceIds(Arrays.asList(instanceId))
                        .build();
            }
        }catch (Exception e){
            log.info("阿卡麦-创建实例异常:{}",e.getMessage());
            e.printStackTrace();
            return CreateVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }

    }


    /**
     * 查询主机创建信息
     * @param querySO
     * @throws Exception
     */
    public QueryVO createQuery(QuerySO querySO){
        String instanceId = querySO.getInstanceIds().get(0);
        String url = "https://api.linode.com/v4/linode/instances";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        headerMap.put("X-Filter","{\"id\":\""+instanceId+"\"}");
        String result = HttpRequest.sendGet(url,headerMap);
        JSONObject json = JSONObject.fromObject(result);
        if(json.get("errors") != null){
            log.info("阿卡麦-查询实例参数:{}",instanceId);
            log.info("阿卡麦-查询实例结果:{}",result);
            return QueryVO .builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(result)
                    .build();
        }else{
            JSONArray dataArray = json.getJSONArray("data");
            if(dataArray.size() > 0){

                JSONObject data = dataArray.getJSONObject(0);
                String state = data.getString("status");

                /**
                 running offline booting busy rebooting shutting_down provisioning deleting migrating rebuilding cloning restoring stopped billing_suspension
                 * **/
                String powerState;
                if("booting".toLowerCase().equals(state.toLowerCase()) || "rebooting".toLowerCase().equals(state.toLowerCase())
                        || "shutting_down".toLowerCase().equals(state.toLowerCase()) || "rebuilding".toLowerCase().equals(state.toLowerCase())){
                    /** 执行中 **/
                    powerState = PowerStateEnum.EXECUTION.getVal();
                }else if("running".toLowerCase().equals(state.toLowerCase())){
                    /** 开机中 **/
                    powerState =  PowerStateEnum.RUNNING.getVal();
                }else if("offline".toLowerCase().equals(state.toLowerCase())){
                    /** 已关机 **/
                    powerState = PowerStateEnum.HALTED.getVal();
                }else{
                    /** 未知状态标签 **/
                    powerState = state;
                }

                Map<String , QueryDetailVO> queryDetailMap = new HashMap<>();
                if("running".equals(state.toLowerCase())) {//主机状态运行中
                    JSONArray ipArray = data.getJSONArray("ipv4");
                    String publicIp = ipArray.getString(0);
                    String privateIp = ipArray.size() > 1 ? data.getJSONArray("ipv4").getString(1) : "";

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

                }else{
                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(instanceId)
                            .status(0)
                            .msg("阿卡麦实例创建等待状态:"+state)
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(instanceId,odvo);
                }


                return QueryVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .queryDetailMap(queryDetailMap)
                        .build();
            }else{
                return QueryVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg("暂无数据")
                        .build();
            }
        }

    }

    /**
     * 查询实例信息
     * @param querySO
     * @return
     */
    public QueryVO query(QuerySO querySO){
        String instanceId = querySO.getInstanceIds().get(0);
        String url = "https://api.linode.com/v4/linode/instances";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        headerMap.put("X-Filter","{\"id\":\""+instanceId+"\"}");
        String result = HttpRequest.sendGet(url,headerMap);
        JSONObject json = JSONObject.fromObject(result);
        if(json.get("errors") != null){
            log.info("阿卡麦-查询实例参数:{}",instanceId);
            log.info("阿卡麦-查询实例结果:{}",result);
            return QueryVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(result)
                    .build();
        }else{
            JSONArray dataArray = json.getJSONArray("data");
            Map<String , QueryDetailVO> queryDetailMap = new HashMap<>();

            for(Object obj : dataArray){
                JSONObject data = JSONObject.fromObject(obj);
                String state = data.getString("status");
                /**
                 running offline booting busy rebooting shutting_down provisioning deleting migrating rebuilding cloning restoring stopped billing_suspension
                 * **/
                String powerState;
                if("booting".toLowerCase().equals(state.toLowerCase()) || "rebooting".toLowerCase().equals(state.toLowerCase())
                        || "shutting_down".toLowerCase().equals(state.toLowerCase()) || "rebuilding".toLowerCase().equals(state.toLowerCase())){
                    /** 执行中 **/
                    powerState = PowerStateEnum.EXECUTION.getVal();
                }else if("running".toLowerCase().equals(state.toLowerCase())){
                    /** 开机中 **/
                    powerState =  PowerStateEnum.RUNNING.getVal();
                }else if("offline".toLowerCase().equals(state.toLowerCase())){
                    /** 已关机 **/
                    powerState = PowerStateEnum.HALTED.getVal();
                }else{
                    /** 未知状态标签 **/
                    powerState = state;
                }

                if("running".equals(state.toLowerCase())) {//主机状态运行中
                    JSONArray ipArray = data.getJSONArray("ipv4");
                    String publicIp = ipArray.getString(0);
                    String privateIp = ipArray.size() > 1 ? data.getJSONArray("ipv4").getString(1) : "";

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

                }else{
                    QueryDetailVO odvo = QueryDetailVO.builder()
                            .serviceNo(instanceId)
                            .status(0)
                            .msg("阿卡麦实例创建等待状态:"+state)
                            .powerState(powerState)
                            .build();
                    queryDetailMap.put(instanceId,odvo);
                }
            }

            return QueryVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .queryDetailMap(queryDetailMap)
                    .build();
        }
    }

    /**
     * 续费（没有到期时间-默认续费成功）
     * @param renewSO
     * @return
     */
    public RenewVO renew(RenewSO renewSO){
        return RenewVO.builder()
                .code(CommonUtil.SUCCESS_CODE)
                .msg(CommonUtil.SUCCESS_MSG)
                .build();
    }


    /**
     * 开机
     * @param startSO
     * @return
     * @throws Exception
     */
    public StartVO start(StartSO startSO){
        String instanceId = startSO.getInstanceId();
        String url = "https://api.linode.com/v4/linode/instances/"+instanceId+"/boot";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        headerMap.put("X-Filter","{\"id\":\""+instanceId+"\"}");

        try{
            String result = HttpRequest.postJson(url,"{}",headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("errors") != null){
                log.info("阿卡麦-实例开机参数:{}",instanceId);
                log.info("阿卡麦-实例开机结果:{}",result);
                return StartVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }else{
                return StartVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }
        }catch (Exception e){
            log.info("阿卡麦-实例[{}]开机异常:{}",instanceId,e.getMessage());
            e.printStackTrace();
            return StartVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
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
        String instanceId = rebootSO.getInstanceId();
        String url = "https://api.linode.com/v4/linode/instances/"+instanceId+"/reboot";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        headerMap.put("X-Filter","{\"id\":\""+instanceId+"\"}");

        try{
            String result = HttpRequest.postJson(url,"{}",headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("errors") != null){
                log.info("阿卡麦-实例重启参数:{}",instanceId);
                log.info("阿卡麦-实例重启结果:{}",result);
                return RebootVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }else{
                return RebootVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }
        }catch (Exception e){
            log.info("阿卡麦-实例[{}]重启异常:{}",instanceId,e.getMessage());
            e.printStackTrace();
            return RebootVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
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
        String instanceId = stopSO.getInstanceId();
        String url = "https://api.linode.com/v4/linode/instances/"+instanceId+"/shutdown";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        headerMap.put("X-Filter","{\"id\":\""+instanceId+"\"}");

        try{
            String result = HttpRequest.postJson(url,"{}",headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("errors") != null){
                log.info("阿卡麦-实例关机参数:{}",instanceId);
                log.info("阿卡麦-实例关机结果:{}",result);
                return StopVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }else{
                return StopVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }
        }catch (Exception e){
            log.info("阿卡麦-实例[{}]关机异常:{}",instanceId,e.getMessage());
            e.printStackTrace();
            return StopVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
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
        String instanceId = reinstallSO.getInstanceId();
        String url = "https://api.linode.com/v4/linode/instances/"+instanceId+"/rebuild";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        Map<String,Object> filter = new HashMap<>();
        filter.put("id",instanceId);

        headerMap.put("X-Filter",JSONObject.fromObject(filter).toString());
        Map<String,Object> param = new HashMap<>();
        param.put("image",reinstallSO.getImageId());
        param.put("root_pass",reinstallSO.getPwd());
        param.put("booted",true);

        try{
            String result = HttpRequest.postJson(url,JSONObject.fromObject(param).toString(),headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("errors") != null){
                log.info("阿卡麦-实例[{}]重装参数:{}",instanceId,JSONObject.fromObject(filter).toString());
                log.info("阿卡麦-实例[{}]重装结果:{}",instanceId,result);
                return ReinstallVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }else{
                return ReinstallVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }
        }catch (Exception e){
            log.info("阿卡麦-实例[{}]重装异常:{}",instanceId,e.getMessage());
            e.printStackTrace();
            return ReinstallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 销毁
     * 销毁前先解绑防火墙并清理无主防火墙（不阻塞销毁），与Rcloud销毁时序一致
     * @param destroySO
     * @return
     */
    public DestroyVO destroy(DestroySO destroySO){
        String instanceId = destroySO.getInstanceId();
        String url = "https://api.linode.com/v4/linode/instances/"+instanceId;
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        try{
            /** 销毁前解绑防火墙：实例存活时才能查到绑定关系，解绑后无主的防火墙顺手清理 **/
            try{
                unbindAndCleanFirewalls(instanceId);
            }catch (Exception e){
                log.info("阿卡麦-实例{}销毁前防火墙解绑异常（不阻塞销毁）：{}", instanceId, e.getMessage());
            }
            String result = HttpRequest.sendDelete(url,headerMap);
            JSONObject json = parseJson(result);
            /** Linode删除成功返回空体或{}，均无errors **/
            if(json == null || json.get("errors") != null){
                log.info("阿卡麦-实例销毁参数:{}",instanceId);
                log.info("阿卡麦-实例销毁结果:{}",result);
                return DestroyVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }else{
                log.info("阿卡麦-实例[{}]销毁成功", instanceId);
                return DestroyVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }
        }catch (Exception e){
            log.info("阿卡麦-实例[{}]销毁异常:{}",instanceId,e.getMessage());
            e.printStackTrace();
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
    public UpdateAuteRenewVO updateAuteRenew(UpdateAuteRenewSO updateAuteRenewSO){
        return UpdateAuteRenewVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿卡麦-不支持此功能")
                .build();
    }


    /**
     * 更新主机密码
     * @param updatePwdSO
     * @return
     * @throws Exception
     */
    public UpdatePwdVO updatePwd(UpdatePwdSO updatePwdSO){
        String instanceId = updatePwdSO.getInstanceId();
        String url = "https://api.linode.com/v4/linode/instances/"+instanceId+"/password";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        headerMap.put("X-Filter","{\"id\":\""+instanceId+"\"}");

        try{
            Map<String,Object> param = new HashMap<>();
            param.put("root_pass",updatePwdSO.getPwd());
            String result = HttpRequest.postJson(url,JSONObject.fromObject(param).toString(),headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("errors") != null){
                log.info("阿卡麦-实例[{}]改密参数:{}",instanceId,JSONObject.fromObject(param).toString());
                log.info("阿卡麦-实例[{}]改密结果:{}",instanceId,result);
                return UpdatePwdVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }else{
                return UpdatePwdVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }
        }catch (Exception e){
            log.info("阿卡麦-实例[{}]改密异常:{}",instanceId,e.getMessage());
            e.printStackTrace();
            return UpdatePwdVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }


    /**
     * 创建防火墙（Linode Cloud Firewall独立实体）
     * 入站：默认DROP，固定放行SSH 22 + 上游指定端口；出站：全部放行
     * @param createSecuritySO
     * @return
     * @throws Exception
     */
    @Override
    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO) throws Exception {
        String label = sanitizeLabel(createSecuritySO.getName());
        Set<String> seen = new HashSet<>();
        List<JSONObject> inbound = new ArrayList<>();
        ensureSshRule(inbound, seen);
        List<CreateSecuritySO.FirewallRule> rules = createSecuritySO.getRules();
        if(rules != null && !rules.isEmpty()){
            /** 新接口格式：按每条规则的协议/端口/源IP生成 **/
            for(CreateSecuritySO.FirewallRule rule : rules){
                String protocol = StringUtils.isNotEmpty(rule.getProtocol()) ? rule.getProtocol().trim().toUpperCase() : "TCP";
                if("ALL".equals(protocol)){
                    addPortRule(inbound, seen, "TCP", rule.getPort(), rule.getSource());
                    addPortRule(inbound, seen, "UDP", rule.getPort(), rule.getSource());
                }else{
                    addPortRule(inbound, seen, protocol, rule.getPort(), rule.getSource());
                }
            }
        }else{
            /** 旧格式：仅传port字符串，默认TCP+UDP双协议（与Rcloud一致） **/
            for(String port : splitPorts(createSecuritySO.getPort())){
                addPortRule(inbound, seen, "TCP", port, "0.0.0.0/0");
                addPortRule(inbound, seen, "UDP", port, "0.0.0.0/0");
            }
        }
        JSONObject param = new JSONObject();
        param.put("label", label);
        param.put("rules", buildRulesObject(inbound));

        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        try{
            log.info("阿卡麦-创建防火墙[{}]请求:{}", label, param);
            String result = HttpRequest.postJson(FIREWALL_URL, param.toString(), headerMap);
            log.info("阿卡麦-创建防火墙[{}]结果:{}", label, result);
            JSONObject json = parseJson(result);
            if(json == null || json.get("errors") != null){
                return CreateSecurityVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }
            return CreateSecurityVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .fwId(json.getString("id"))
                    .build();
        }catch (Exception e){
            log.info("阿卡麦-创建防火墙[{}]异常:{}", label, e.getMessage());
            e.printStackTrace();
            return CreateSecurityVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 查询防火墙：优先按名称label匹配；fwId先按防火墙ID查（业务约定本平台fwId可能承载实例ID，查不到再按实例绑定关系兜底）；instanceId按实例绑定关系查
     * @param queryFirewallSO
     * @return
     * @throws Exception
     */
    @Override
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception {
        try{
            JSONObject firewallJson = null;
            //1、按名称查（X-Filter按label精确匹配）
            if(StringUtils.isNotEmpty(queryFirewallSO.getName())){
                JSONObject filter = new JSONObject();
                filter.put("label", sanitizeLabel(queryFirewallSO.getName()));
//                log.info("阿卡麦-按名查防火墙[{}]，过滤条件:{}", queryFirewallSO.getName(), filter);
                String result = firewallsGet(FIREWALL_URL, filter.toString());
//                log.info("阿卡麦-按名查防火墙[{}]结果:{}", queryFirewallSO.getName(), result);
                JSONObject json = parseJson(result);
                if(json == null || json.get("errors") != null){
                    return QueryFirewallVO.builder()
                            .code(CommonUtil.FAIL_CODE)
                            .msg(result)
                            .build();
                }
                JSONArray dataArray = json.getJSONArray("data");
                if(dataArray.size() > 0){
                    firewallJson = dataArray.getJSONObject(0);
                }
            }
            //2、按fwId查（先按防火墙ID查，查不到再按实例绑定关系兜底）
            if(firewallJson == null && StringUtils.isNotEmpty(queryFirewallSO.getFwId())){
                firewallJson = firstBoundFirewallCompat(queryFirewallSO.getFwId());
            }
            //3、按实例ID查绑定的防火墙
            if(firewallJson == null && StringUtils.isNotEmpty(queryFirewallSO.getInstanceId())){
                firewallJson = firstBoundFirewall(queryFirewallSO.getInstanceId());
            }

            if(firewallJson == null){
                /** 没查到：返回成功但fwId为空，业务层据此走新建流程 **/
//                log.info("阿卡麦-防火墙按名/ID/实例绑定均未查到，业务层将走新建流程");
                return QueryFirewallVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg("查询成功")
                        .build();
            }
            log.info("阿卡麦-查到防火墙:{}", firewallJson.optString("id"));
            return parseFirewallData(firewallJson);
        }catch (Exception e){
            log.info("阿卡麦-查防火墙异常:{}", e.getMessage());
            e.printStackTrace();
            return QueryFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    @Override
    public CreateFirewallTemplateRulesVO createFirewallTemplateRules(CreateFirewallTemplateRulesSO so) {
        return CreateFirewallTemplateRulesVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿卡麦-不支持此功能 ")
                .build();
    }

    /**
     * 绑定防火墙（POST devices，type=linode；已绑定则直接成功，幂等）
     * @param grantFirewallSO
     * @return
     * @throws Exception
     */
    @Override
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO) throws Exception {
        String firewallId = grantFirewallSO.getGroupId();
        String instanceId = grantFirewallSO.getInstanceId();
        try{
            JSONArray devices = listDevices(firewallId);
            Integer boundDeviceId = devices == null ? null : findDeviceId(devices, instanceId);
            if(boundDeviceId == null){
                JSONObject param = new JSONObject();
                param.put("type", "linode");//固定枚举类型
                param.put("id", Integer.parseInt(instanceId));
                Map<String,String> headerMap = new HashMap<>();
                headerMap.put("Authorization","Bearer "+token);
                /** 云API偶发瞬时错误（如刚创建的防火墙立刻绑定撞上就绪窗口回Unauthorized）：自动重试，最多3次、间隔1秒 **/
                String result = null;
                JSONObject json = null;
                for(int attempt = 1; attempt <= 3; attempt++){
                    result = HttpRequest.postJson(FIREWALL_URL + "/" + firewallId + "/devices", param.toString(), headerMap);
                    json = parseJson(result);
                    if(json != null && json.get("errors") == null){
                        if(attempt > 1){
                            log.info("阿卡麦-绑定防火墙[{}->{}]第{}次重试成功", firewallId, instanceId, attempt);
                        }
                        break;
                    }
                    log.info("阿卡麦-绑定防火墙[{}->{}]第{}次尝试失败:{}", firewallId, instanceId, attempt, result);
                    if(attempt < 3){
                        try{
                            Thread.sleep(1000);
                        }catch (InterruptedException ie){
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                if(json == null || json.get("errors") != null){
                    log.info("阿卡麦-绑定防火墙[{}->{}]重试后仍失败:{}", firewallId, instanceId, result);
                    return GrantFirewallVO.builder()
                            .success(false)
                            .code(CommonUtil.FAIL_CODE)
                            .msg(result)
                            .build();
                }
            }
            if(boundDeviceId != null){
                log.info("阿卡麦-防火墙[{}]已绑定实例[{}]，跳过重复绑定", firewallId, instanceId);
            }else{
                log.info("阿卡麦-防火墙[{}]绑定实例[{}]成功", firewallId, instanceId);
            }
            return GrantFirewallVO.builder()
                    .success(true)
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("阿卡麦-绑定防火墙[{}->{}]异常:{}", firewallId, instanceId, e.getMessage());
            e.printStackTrace();
            return GrantFirewallVO.builder()
                    .success(false)
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
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
                .msg("阿卡麦-不支持此功能")
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
                .msg("阿卡麦-不支持此功能")
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
                .msg("阿卡麦-不支持此功能")
                .build();
    }


    /**
     * 更新防火墙规则（PUT rules全量替换：入站默认DROP只放行规则内端口，出站全放）
     * @param updateFirewallSO
     * @return
     * @throws Exception
     */
    @Override
    public UpdateFirewallVO updateFirewall(UpdateFirewallSO updateFirewallSO) throws Exception {
        String firewallId = updateFirewallSO.getFwId();
        try{
            /** 全量规则：固定放行SSH 22 + 上游传入规则（协议|端口|源IP|动作|优先级|备注） **/
            Set<String> seen = new HashSet<>();
            List<JSONObject> inbound = new ArrayList<>();
            ensureSshRule(inbound, seen);
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
                    if("ALL".equals(protocol)){
                        addPortRule(inbound, seen, "TCP", parts[1].trim(), parts[2].trim());
                        addPortRule(inbound, seen, "UDP", parts[1].trim(), parts[2].trim());
                    }else{
                        if(!"TCP".equals(protocol) && !"UDP".equals(protocol) && !"ICMP".equals(protocol)){
                            protocol = "TCP";
                        }
                        addPortRule(inbound, seen, protocol, parts[1].trim(), parts[2].trim());
                    }
                }
            }
            JSONObject param = buildRulesObject(inbound);

            Map<String,String> headerMap = new HashMap<>();
            headerMap.put("Authorization","Bearer "+token);
            String result = HttpRequest.putJson(FIREWALL_URL + "/" + firewallId + "/rules", param.toString(), headerMap);
            JSONObject json = parseJson(result);
            if(json == null || json.get("errors") != null){
                log.info("阿卡麦-更新防火墙[{}]规则结果:{}", firewallId, result);
                return UpdateFirewallVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }
            return UpdateFirewallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("阿卡麦-更新防火墙[{}]规则异常:{}", firewallId, e.getMessage());
            e.printStackTrace();
            return UpdateFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
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
        String firewallId = deleteFirewallSO.getFwId();
        try{
            String result = sendDeleteByUrl(FIREWALL_URL + "/" + firewallId);
            JSONObject json = parseJson(result);
            /** Linode删除成功返回空体或{}，均无errors **/
            if(json == null || json.get("errors") != null){
                log.info("阿卡麦-删除防火墙[{}]结果:{}", firewallId, result);
                return DeleteFirewallVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }
            return DeleteFirewallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("阿卡麦-删除防火墙[{}]异常:{}", firewallId, e.getMessage());
            e.printStackTrace();
            return DeleteFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    /**
     * 解绑主机的所有安全组并清理无主防火墙
     * @param serviceNo
     * @return
     * @throws Exception
     */
    @Override
    public RevokeFirewallVO unbindAndCleanFirewalls(String serviceNo) throws Exception {
        try{
            String result = firewallsGet(BASE_URL + "/linode/instances/" + serviceNo + "/firewalls", null);
            JSONObject json = parseJson(result);
            if(json == null || json.get("errors") != null){
                log.info("阿卡麦-查实例[{}]绑定防火墙结果:{}", serviceNo, result);
                return RevokeFirewallVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }
            JSONArray dataArray = json.getJSONArray("data");
            for(int i = 0; i < dataArray.size(); i++){
                String firewallId = dataArray.getJSONObject(i).getString("id");
                //解绑该实例的设备
                JSONArray devices = listDevices(firewallId);
                Integer deviceId = devices == null ? null : findDeviceId(devices, serviceNo);
                if(deviceId != null){
                    String delResult = sendDeleteByUrl(FIREWALL_URL + "/" + firewallId + "/devices/" + deviceId);
                    JSONObject delJson = parseJson(delResult);
                    if(delJson == null || delJson.get("errors") != null){
                        log.info("阿卡麦-解绑防火墙[{}]设备[{}]结果:{}", firewallId, deviceId, delResult);
                        return RevokeFirewallVO.builder()
                                .code(CommonUtil.FAIL_CODE)
                                .msg(delResult)
                                .build();
                    }else{
                        log.info("阿卡麦-防火墙[{}]解绑设备[{}]成功", firewallId, deviceId);
                    }
                }
                //解绑后防火墙已无绑定设备则删除（无主清理）
                JSONArray remaining = listDevices(firewallId);
                if(remaining != null && remaining.size() == 0){
                    String delResult = sendDeleteByUrl(FIREWALL_URL + "/" + firewallId);
                    JSONObject delJson = parseJson(delResult);
                    if(delJson == null || delJson.get("errors") != null){
                        log.info("阿卡麦-清理无主防火墙[{}]结果:{}", firewallId, delResult);
                    }else{
                        log.info("阿卡麦-清理无主防火墙[{}]成功", firewallId);
                    }
                }
            }
            return RevokeFirewallVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .build();
        }catch (Exception e){
            log.info("阿卡麦-解绑清理实例[{}]防火墙异常:{}", serviceNo, e.getMessage());
            e.printStackTrace();
            return RevokeFirewallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    // ============================== 防火墙辅助方法 ==============================

    /** Linode防火墙label规则：3-32位，字母开头，仅字母/数字/_/-，且不允许连续两个_/-/.；
     *  不合规字符统一转_后压缩连续段、去首尾残留（查询与创建用同一规则保证按名复用）；
     *  中文名清洗后会变形，追加原名的hash后缀保证同名同label、异名异label，hash放尾部但截断时保留hash段 **/
    private String sanitizeLabel(String name){
        String original = (name == null ? "" : name).trim();
        String s = original.replaceAll("[^a-zA-Z0-9_-]", "_");
        s = s.replaceAll("[_\\-.]{2,}", "_");
        s = s.replaceAll("^[_.\\-]+", "").replaceAll("[_.\\-]+$", "");
        if(s.isEmpty()){
            s = "akm";
        }
        if(!s.equals(original)){
            String suffix = "h" + Math.abs(original.hashCode());
            int keep = 32 - suffix.length() - 1;
            if(keep > s.length()){
                keep = s.length();
            }
            if(keep < 1){
                keep = 1;
            }
            String head = s.substring(0, keep).replaceAll("[_.\\-]+$", "");
            if(head.isEmpty()){
                head = "akm";
            }
            s = head + "-" + suffix;
        }
        if(!Character.isLetter(s.charAt(0))){
            s = "fw-" + s;
        }
        if(s.length() > 32){
            s = s.substring(0, 32);
        }
        if(s.length() < 3){
            s = s + "-fw";
        }
        return s;
    }

    /** 拆分端口串："80,443,8080-8090" → ["80","443","8080-8090"]（Linode原生支持范围写法） **/
    private List<String> splitPorts(String portStr){
        List<String> list = new ArrayList<>();
        if(StringUtils.isEmpty(portStr)){
            return list;
        }
        for(String p : portStr.split(",")){
            String item = p.trim();
            if(StringUtils.isNotEmpty(item)){
                list.add(item);
            }
        }
        return list;
    }

    /** 追加入站放行规则（ICMP无端口；按协议+端口去重） **/
    private void addPortRule(List<JSONObject> inbound, Set<String> seen, String protocol, String port, String source){
        if("ICMP".equals(protocol) || StringUtils.isEmpty(port)){
            if(seen.add(protocol + ":")){
                inbound.add(buildInboundRule(protocol, null, source, "ACCEPT"));
            }
            return;
        }
        String portTrim = port.trim();
        if(seen.add(protocol + ":" + portTrim)){
            inbound.add(buildInboundRule(protocol, portTrim, source, "ACCEPT"));
        }
    }

    /** 固定放行SSH 22（TCP） **/
    private void ensureSshRule(List<JSONObject> inbound, Set<String> seen){
        if(seen.add("TCP:22")){
            inbound.add(buildInboundRule("TCP", "22", "0.0.0.0/0", "ACCEPT"));
        }
    }

    /** 构建单条Linode规则：{protocol, ports?, addresses, action}，IPv6源走ipv6字段 **/
    private JSONObject buildInboundRule(String protocol, String ports, String source, String action){
        JSONObject rule = new JSONObject();
        rule.put("protocol", protocol);
        if(StringUtils.isNotEmpty(ports)){
            rule.put("ports", ports);
        }
        JSONObject addresses = new JSONObject();
        if(StringUtils.isNotEmpty(source) && source.contains(":")){
            addresses.put("ipv6", Arrays.asList(source.trim()));
        }else{
            addresses.put("ipv4", Arrays.asList(StringUtils.isNotEmpty(source) ? source.trim() : "0.0.0.0/0"));
        }
        rule.put("addresses", addresses);
        rule.put("action", action);
        return rule;
    }

    /** Linode rules对象：入站默认DROP放行指定端口，出站ACCEPT全放（liebao出站限死会挡机器主动外连，不照抄） **/
    private JSONObject buildRulesObject(List<JSONObject> inbound){
        JSONObject rules = new JSONObject();
        rules.put("inbound_policy", "DROP");
        rules.put("inbound", JSONArray.fromObject(inbound));
        rules.put("outbound_policy", "ACCEPT");
        rules.put("outbound", new JSONArray());
        return rules;
    }

    /** 解析响应：空体返回{}（DELETE成功场景），非JSON返回null（传输失败） **/
    private JSONObject parseJson(String result){
        try{
            if(StringUtils.isEmpty(result)){
                return new JSONObject();
            }
            return JSONObject.fromObject(result);
        }catch (Exception e){
            return null;
        }
    }

    /** 带鉴权头（可选X-Filter）的GET **/
    private String firewallsGet(String url, String xFilter){
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        if(StringUtils.isNotEmpty(xFilter)){
            headerMap.put("X-Filter", xFilter);
        }
        return HttpRequest.sendGet(url, headerMap);
    }

    /** 带鉴权头的DELETE **/
    private String sendDeleteByUrl(String url){
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        return HttpRequest.sendDelete(url, headerMap);
    }

    /** 查实例绑定的防火墙列表，取第一个，没有返回null **/
    private JSONObject firstBoundFirewall(String instanceId){
        String result = firewallsGet(BASE_URL + "/linode/instances/" + instanceId + "/firewalls", null);
        JSONObject json = parseJson(result);
        if(json == null || json.get("errors") != null){
            log.info("阿卡麦-查实例[{}]绑定防火墙结果:{}", instanceId, result);
            return null;
        }
        JSONArray dataArray = json.getJSONArray("data");
        if(dataArray.size() > 0){
            return dataArray.getJSONObject(0);
        }
        return null;
    }

    /** 查防火墙的绑定设备列表，失败返回null **/
    private JSONArray listDevices(String firewallId){
        String result = firewallsGet(FIREWALL_URL + "/" + firewallId + "/devices", null);
        JSONObject json = parseJson(result);
        if(json == null || json.get("errors") != null){
            log.info("阿卡麦-查防火墙[{}]设备列表结果:{}", firewallId, result);
            return null;
        }
        return json.getJSONArray("data");
    }

    /** 从设备列表中找绑定指定实例的设备ID，未绑定返回null（兼容entity/linode两种响应结构） **/
    private Integer findDeviceId(JSONArray devices, String instanceId){
        int linodeId;
        try{
            linodeId = Integer.parseInt(instanceId);
        }catch (NumberFormatException e){
            return null;
        }
        for(int i = 0; i < devices.size(); i++){
            JSONObject device = devices.getJSONObject(i);
            Integer deviceLinodeId = deviceLinodeId(device);
            if(deviceLinodeId != null && deviceLinodeId == linodeId){
                return device.getInt("id");
            }
        }
        return null;
    }

    /** 设备ID便捷匹配：先entity.id后linode.id **/
    private Integer deviceLinodeId(JSONObject device){
        if(device.containsKey("entity") && device.get("entity") instanceof JSONObject){
            return device.getJSONObject("entity").optInt("id");
        }
        if(device.containsKey("linode") && device.get("linode") instanceof JSONObject){
            return device.getJSONObject("linode").optInt("id");
        }
        return null;
    }

    /** 按防火墙ID查：先按防火墙ID查，查不到再按实例绑定关系兜底 **/
    private JSONObject firstBoundFirewallCompat(String fwId){
        JSONObject firewallJson = null;
        if(fwId.matches("\\d+")){
            JSONObject filter = new JSONObject();
            filter.put("id", Integer.parseInt(fwId));
            String result = firewallsGet(FIREWALL_URL, filter.toString());
            JSONObject json = parseJson(result);
            if(json != null && json.get("errors") == null && json.getJSONArray("data").size() > 0){
                firewallJson = json.getJSONArray("data").getJSONObject(0);
            }
        }
        if(firewallJson == null){
            firewallJson = firstBoundFirewall(fwId);
        }
        return firewallJson;
    }

    /** 防火墙JSON转QueryFirewallVO：groupId与fwId同值（Linode无组概念），入站规则转FirewallRule列表 **/
    private QueryFirewallVO parseFirewallData(JSONObject firewallJson){
        String fwId = firewallJson.getString("id");
        QueryFirewallVO vo = QueryFirewallVO.builder()
                .fwId(fwId)
                .groupId(fwId)
                .name(firewallJson.getString("label"))
                .rules(new ArrayList<>())
                .code(CommonUtil.SUCCESS_CODE)
                .msg("查询成功")
                .build();
        if(firewallJson.containsKey("rules") && firewallJson.get("rules") instanceof JSONObject){
            JSONObject rules = firewallJson.getJSONObject("rules");
            if(rules.containsKey("inbound") && rules.get("inbound") instanceof JSONArray){
                JSONArray inbound = rules.getJSONArray("inbound");
                for(int i = 0; i < inbound.size(); i++){
                    JSONObject rule = inbound.getJSONObject(i);
                    FirewallRule entity = new FirewallRule();
                    entity.setFirewallId(fwId);
                    entity.setProtocol(rule.getString("protocol"));
                    if(rule.containsKey("ports")){
                        entity.setPort(rule.getString("ports"));
                    }
                    if(rule.containsKey("addresses") && rule.get("addresses") instanceof JSONObject){
                        JSONObject addresses = rule.getJSONObject("addresses");
                        if(addresses.containsKey("ipv4") && addresses.getJSONArray("ipv4").size() > 0){
                            entity.setIpAddress(addresses.getJSONArray("ipv4").getString(0));
                        }else if(addresses.containsKey("ipv6") && addresses.getJSONArray("ipv6").size() > 0){
                            entity.setIpAddress(addresses.getJSONArray("ipv6").getString(0));
                        }
                    }
                    entity.setAction(rule.getString("action"));
                    entity.setDirection("inbound");
                    vo.getRules().add(entity);
                }
            }
        }
        return vo;
    }
}
