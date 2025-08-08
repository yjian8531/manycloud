package com.core.manycloudtimer.job;

import com.core.manycloudcommon.caller.BaseCaller;
import com.core.manycloudcommon.caller.so.DestroySO;
import com.core.manycloudcommon.caller.so.StopSO;
import com.core.manycloudcommon.caller.vo.DestroyVO;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.enums.PlatformLabelEnum;
import com.core.manycloudcommon.enums.PowerStateEnum;
import com.core.manycloudcommon.enums.SyslogTypeEnum;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.model.SyslogModel;
import com.core.manycloudcommon.utils.*;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 实例到期定时器
 */
@Slf4j
@Component      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class WxTokenTimer {

    @Autowired
    private SysParamMapper sysParamMapper;


    @Autowired
    private Environment env;

    /**
     * 自动更新微信公众号token
     */
    @Scheduled(cron="0 0 */1 * * ?")
    public void autoUpdateAccessToken(){

        String accessToken = getAccessToken(env.getProperty("wxm.appid"),env.getProperty("wxm.appsecret"));
        if(accessToken != null){
            SysParam sysParam = sysParamMapper.selectByTail("WX_ACCESS_TOKEN");
            if(sysParam == null){
                sysParam = new SysParam();
                sysParam.setName("微信公众号token");
                sysParam.setTail("WX_ACCESS_TOKEN");
                sysParam.setVal(accessToken);
                sysParam.setCreateTime(new Date());
                sysParam.setUpdateTime(new Date());
                sysParamMapper.insertSelective(sysParam);
            }else{
                sysParam.setVal(accessToken);
                sysParam.setUpdateTime(new Date());
                sysParamMapper.updateByPrimaryKey(sysParam);
            }
        }

    }

    /**
     * 获取微信公众号token
     * @param appid
     * @param secret
     * @return
     */
    public String getAccessToken(String appid,String secret){
        String api = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
        api = api.replace("{appid}",appid).replace("{secret}",secret);
        String str = HttpRequest.sendGet(api);
        JSONObject result = JSONObject.fromObject(str);
        if(result.get("access_token") != null){
            return result.getString("access_token");
        }else{
            log.info("微信公众号获取AccessToken失败:{}",str);
            return null;
        }
    }




}
