package com.core.manycloudcommon.caller;

import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.model.AccountApi;

public interface BaseCaller {

    public static BaseCaller getCaller(AccountApi accountApi){

        BaseCaller caller;

        switch(accountApi.getLabel()){
            case "UCLOUD":
                caller = UcloudCaller.getClient(accountApi);
                break;
            case "RCLOUD":
                caller = RcloudCaller.getClient(accountApi);
                break;
            case "ALIYUN":
                caller = AliyunCaller.getClient(accountApi);
                break;
            case "AWSLS":
                caller = AwsLightSailCaller.getClient(accountApi);
                break;
            case "DOPLA":
                caller = DigitalOceanCaller.getClient(accountApi);
                break;
            case "AKMPLA":
                caller = AkamaiCaller.getClient(accountApi);
                break;
            default:
                caller = null;
        }

        return caller;
    }

    /**
     * 创建实例
     * @param createSO
     * @return
     * @throws Exception
     */
    public CreateVO create(CreateSO createSO)throws Exception;

    /**
     * 查询主机创建信息
     * @param querySO
     * @throws Exception
     */
    public QueryVO createQuery(QuerySO querySO)throws Exception;



    /**
     * 查询主机信息
     * @param querySO
     * @throws Exception
     */
    public QueryVO query(QuerySO querySO)throws Exception;


    /**
     * 续费
     * @param renewSO
     * @return
     * @throws Exception
     */
    public RenewVO renew(RenewSO renewSO)throws Exception;

    /**
     * 开机
     * @param startSO
     * @return
     * @throws Exception
     */
    public StartVO start(StartSO startSO)throws Exception;


    /**
     * 重启
     * @param rebootSO
     * @return
     * @throws Exception
     */
    public RebootVO reboot(RebootSO rebootSO)throws Exception;


    /**
     * 关机
     * @param stopSO
     * @return
     * @throws Exception
     */
    public StopVO stop(StopSO stopSO)throws Exception;


    /**
     * 重装系统
     * @param reinstallSO
     * @return
     * @throws Exception
     */
    public ReinstallVO reinstall(ReinstallSO reinstallSO) throws Exception;


    /**
     * 销毁
     * @param destroySO
     * @return
     */
    public DestroyVO destroy(DestroySO destroySO)throws Exception;


    /**
     * 设置主机自动续费标识
     * @param updateAuteRenewSO
     * @return
     */
    public UpdateAuteRenewVO updateAuteRenew(UpdateAuteRenewSO updateAuteRenewSO)throws Exception;



    /**
     * 更新主机密码
     * @param updatePwdSO
     * @return
     * @throws Exception
     */
    public UpdatePwdVO updatePwd(UpdatePwdSO updatePwdSO) throws Exception;


    /**
     * 创建安全组
     * @param
     * @return
     * @throws Exception
     */
    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO) throws Exception;


    /**
     * 查询安全组
     * @param
     * @return
     * @throws Exception
     */
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception;


    /**
     * 绑定安全组
     * @param
     * @return
     * @throws Exception
     */
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO) throws Exception;



}
