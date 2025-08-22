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
public class AkamaiCaller implements BaseCaller{

    private static Map<String , AkamaiCaller> akamaiCallerMap = new HashMap<>();

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
            return QueryVO.builder()
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
                            .msg("数字海洋实例创建等待状态:"+state)
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
     * @param destroySO
     * @return
     */
    public DestroyVO destroy(DestroySO destroySO){
        String instanceId = destroySO.getInstanceId();
        String url = "https://api.linode.com/v4/linode/instances/"+instanceId;
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Authorization","Bearer "+token);
        try{
            String result = HttpRequest.sendDelete(url,headerMap);
            JSONObject json = JSONObject.fromObject(result);
            if(json.get("errors") != null){
                log.info("阿卡麦-实例销毁参数:{}",instanceId);
                log.info("阿卡麦-实例销毁结果:{}",result);
                return DestroyVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(result)
                        .build();
            }else{
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


    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO) throws Exception {
        return CreateSecurityVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿卡麦-不支持此功能")
                .build();
    }

    @Override
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception {
        return QueryFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿卡麦-不支持此功能")
                .build();
    }

    @Override
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO) throws Exception {
        return GrantFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("阿卡麦-不支持此功能")
                .build();
    }
}
