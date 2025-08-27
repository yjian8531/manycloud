package com.core.manycloudcommon.caller;

import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.enums.PowerStateEnum;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.HttpRequest;
import com.core.manycloudcommon.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DigitalOceanCaller implements BaseCaller{

    private static Map<String , DigitalOceanCaller> digitalOceanCallerMap = new HashMap<>();

    private String token;
    private String account;
    private String region;


    public static DigitalOceanCaller getClient(AccountApi accountApi){
        if(digitalOceanCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId()) == null){
            synchronized(DigitalOceanCaller.class){
                DigitalOceanCaller digitalOceanCaller = new DigitalOceanCaller(accountApi.getRegionId(),accountApi.getAccount(),accountApi.getKeyNo());
                digitalOceanCallerMap.put(accountApi.getAccount()+":"+accountApi.getRegionId(),digitalOceanCaller);
            }
            return digitalOceanCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }else{
            return digitalOceanCallerMap.get(accountApi.getAccount()+":"+accountApi.getRegionId());
        }
    }

    private DigitalOceanCaller(String region, String account, String apiKey){
        this.region = region;
        this.account = account;
        this.token = apiKey;
    }

    public String getImage(){
        String url ="https://api.digitalocean.com/v2/images?per_page=100&page=1&private=true";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        String str = HttpRequest.sendGet(url,headerMap);
        return str;

    }

    /**
     * 创建实例
     * @param createSO
     * @return
     */
    public CreateVO create(CreateSO createSO){
        String instanceName = "do-"+CommonUtil.getRandomStr(12);
        String url = "https://api.digitalocean.com/v2/droplets";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        Map<String,Object> param = new HashMap<>();
        param.put("name",instanceName);
        param.put("region",region);
        param.put("size",createSO.getBundleId());
        param.put("image",createSO.getImageId());
        param.put("user_data","#cloud-config\nchpasswd:\n  list: |\n    root:"+createSO.getPwd()+"\n  expire: False\nssh_pwauth: True");

        try{
            String result = HttpRequest.postJson(url, JSONObject.fromObject(param).toString(),headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("droplet") != null){
                String instanceId = json.getJSONObject("droplet").getString("id");
                return CreateVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .instanceIds(Arrays.asList(instanceId))
                        .build();
            }else{

                log.info("数字海洋-创建实例参数:{}",JSONObject.fromObject(param).toString());
                log.info("数字海洋-创建实例结果:{}",result);
                return CreateVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }

        }catch (Exception e){

            e.printStackTrace();
            return CreateVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }

    public QueryVO createQuery(QuerySO querySO){
        String instanceId = querySO.getInstanceIds().get(0);
        String url = "https://api.digitalocean.com/v2/droplets/"+instanceId;
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        String str = HttpRequest.sendGet(url,headerMap);
        if(StringUtils.isEmpty(str) || JSONObject.fromObject(str).get("droplet") == null){
            log.info("数字海洋-查询实例信息:{}",instanceId);
            log.info("数字海洋-查询实例结果:{}",str);
            return QueryVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }else{
            JSONObject json = JSONObject.fromObject(str).getJSONObject("droplet");
            String state = json.getString("status");
            String powerState;
            if("new".toLowerCase().equals(state)){
                /** 执行中 **/
                powerState = PowerStateEnum.EXECUTION.getVal();
            }else if("active".toLowerCase().equals(state.toLowerCase())){
                /** 开机中 **/
                powerState =  PowerStateEnum.RUNNING.getVal();
            }else if("off".toLowerCase().equals(state.toLowerCase())){
                /** 已关机 **/
                powerState = PowerStateEnum.HALTED.getVal();
            }else{
                /** 未知状态标签 **/
                powerState = state;
            }

            Map<String , QueryDetailVO> queryDetailMap = new HashMap<>();

            if("active".equals(state.toLowerCase())){//主机状态运行中

                String publicIp = null;
                String privateIp = null;
                JSONArray ipv4Array = json.getJSONObject("networks").getJSONArray("v4");
                for(Object ipv4Obj : ipv4Array){
                    JSONObject ipv4 = JSONObject.fromObject(ipv4Obj);
                    if(ipv4.getString("type").equals("public")){
                        publicIp = ipv4.getString("ip_address");
                    }else if(ipv4.getString("type").equals("private")){
                        privateIp = ipv4.getString("ip_address");
                    }
                }

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
                        .msg("数字海洋实例创建等待状态:"+state)
                        .powerState(powerState)
                        .build();
                queryDetailMap.put(instanceId,odvo);
            }

            return QueryVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .queryDetailMap(queryDetailMap)
                    .build();
        }

    }


    public QueryVO query(QuerySO querySO){
        String instanceId = querySO.getInstanceIds().get(0);
        String url = "https://api.digitalocean.com/v2/droplets/"+instanceId;
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        String str = HttpRequest.sendGet(url,headerMap);
        if(StringUtils.isEmpty(str) || JSONObject.fromObject(str).get("droplet") == null){
            log.info("数字海洋-查询实例信息:{}",instanceId);
            log.info("数字海洋-查询实例结果:{}",str);
            return QueryVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }else{
            JSONObject json = JSONObject.fromObject(str).getJSONObject("droplet");
            String state = json.getString("status");
            String powerState;
            if("new".toLowerCase().equals(state)){
                /** 执行中 **/
                powerState = PowerStateEnum.EXECUTION.getVal();
            }else if("active".toLowerCase().equals(state.toLowerCase())){
                /** 开机中 **/
                powerState =  PowerStateEnum.RUNNING.getVal();
            }else if("off".toLowerCase().equals(state.toLowerCase())){
                /** 已关机 **/
                powerState = PowerStateEnum.HALTED.getVal();
            }else{
                /** 未知状态标签 **/
                powerState = state;
            }

            Map<String , QueryDetailVO> queryDetailMap = new HashMap<>();

            String publicIp = null;
            String privateIp = null;
            JSONArray ipv4Array = json.getJSONObject("networks").getJSONArray("v4");
            for(Object ipv4Obj : ipv4Array){
                JSONObject ipv4 = JSONObject.fromObject(ipv4Obj);
                if(ipv4.getString("type").equals("public")){
                    publicIp = ipv4.getString("ip_address");
                }else if(ipv4.getString("type").equals("private")){
                    privateIp = ipv4.getString("ip_address");
                }
            }

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
     */
    public StartVO start(StartSO startSO){
        String url = "https://api.digitalocean.com/v2/droplets/"+startSO.getInstanceId()+"/actions";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        Map<String,Object> param = new HashMap<>();
        param.put("type","power_on");

        try {
            String result = HttpRequest.postJson(url, JSONObject.fromObject(param).toString(), headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("id") != null){
                log.info("数字海洋实例[{}]-开机失败:{}",startSO.getInstanceId(),result);
                String msg = json.getString("message");
                return StartVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(msg)
                        .build();
            }else{
                return StartVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }

        }catch (Exception e){

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
     */
    public RebootVO reboot(RebootSO rebootSO){
        String url = "https://api.digitalocean.com/v2/droplets/"+rebootSO.getInstanceId()+"/actions";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        Map<String,Object> param = new HashMap<>();
        param.put("type","reboot");

        try {
            String result = HttpRequest.postJson(url, JSONObject.fromObject(param).toString(), headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("id") != null){
                log.info("数字海洋实例[{}]-重启失败:{}",rebootSO.getInstanceId(),result);
                String msg = json.getString("message");
                return RebootVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(msg)
                        .build();
            }else{
                return RebootVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }

        }catch (Exception e){
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
     */
    public StopVO stop(StopSO stopSO){
        String url = "https://api.digitalocean.com/v2/droplets/"+stopSO.getInstanceId()+"/actions";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        Map<String,Object> param = new HashMap<>();
        param.put("type","shutdown");

        try {
            String result = HttpRequest.postJson(url, JSONObject.fromObject(param).toString(), headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("id") != null){
                log.info("数字海洋实例[{}]-关机失败:{}",stopSO.getInstanceId(),result);
                String msg = json.getString("message");
                return StopVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(msg)
                        .build();
            }else{
                return StopVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }

        }catch (Exception e){
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
        String url = "https://api.digitalocean.com/v2/droplets/"+reinstallSO.getInstanceId()+"/actions";
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        Map<String,Object> param = new HashMap<>();
        param.put("type","rebuild");
        param.put("image",reinstallSO.getImageId());

        try {
            String result = HttpRequest.postJson(url, JSONObject.fromObject(param).toString(), headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("id") != null){
                log.info("数字海洋实例[{}]-重装失败:{}",reinstallSO.getInstanceId(),result);
                String msg = json.getString("message");
                return ReinstallVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(msg)
                        .build();
            }else{
                return ReinstallVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }

        }catch (Exception e){
            e.printStackTrace();
            return ReinstallVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(e.getMessage())
                    .build();
        }
    }


    /**
     * 销毁
     * @param destroySO
     * @return
     */
    public DestroyVO destroy(DestroySO destroySO){
        String url = "https://api.digitalocean.com/v2/droplets/"+destroySO.getInstanceId();
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        try{
            String result = HttpRequest.sendDelete(url,headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("id") != null){
                log.info("数字海洋实例[{}]-销毁失败:{}",destroySO.getInstanceId(),result);
                String msg = json.getString("message");
                return DestroyVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(msg)
                        .build();
            }else{
                return DestroyVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            }
        }catch (Exception e){
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
                .msg("数字海洋-不支持此功能")
                .build();
    }


    /**
     * 更新主机密码
     * @param updatePwdSO
     * @return
     * @throws Exception
     */
    public UpdatePwdVO updatePwd(UpdatePwdSO updatePwdSO){
        return UpdatePwdVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("数字海洋-不支持此功能")
                .build();
    }

    @Override
    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO) throws Exception {
        return CreateSecurityVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("数字海洋-不支持此功能")
                .build();
    }

    @Override
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception {
        return  QueryFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("数字海洋-不支持此功能")
                .build();
    }

    @Override
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO) throws Exception {
        return  GrantFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("数字海洋-不支持此功能")
                .build();
    }

    @Override
    public ClusterVO queryClusterList(ClusterListSO clusterListSO) throws Exception {
        return ClusterVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("数字海洋-不支持此功能")
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
                .msg("数字海洋-不支持此功能")
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
                .msg("数字海洋-不支持此功能")
                .build();
    }

}
