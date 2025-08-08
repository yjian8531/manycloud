package com.core.manycloudadmin.service.impl;

import com.core.manycloudadmin.service.AdminService;
import com.core.manycloudadmin.so.admin.LoginSO;
import com.core.manycloudadmin.so.admin.QueryAdminLogSO;
import com.core.manycloudcommon.entity.AdminInfo;
import com.core.manycloudcommon.entity.AdminLog;
import com.core.manycloudcommon.mapper.AdminInfoMapper;
import com.core.manycloudcommon.mapper.AdminLogMapper;
import com.core.manycloudcommon.utils.*;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminInfoMapper adminInfoMapper;

    @Autowired
    private AdminLogMapper adminLogMapper;

    /**
     * 登录
     * @param loginSO
     * @return
     */
    public ResultMessage login(LoginSO loginSO){
        AdminInfo adminInfo = adminInfoMapper.selectByAccount(loginSO.getAccount());
        if(adminInfo != null){
            if(MD5.MD5Encode(MD5.MD5Encode(MD5.MD5Encode(loginSO.getPwd()))).equals(adminInfo.getLoginPwd())){
                adminInfo.setLoginTime(new Date());
                adminInfoMapper.updateByPrimaryKeySelective(adminInfo);

                /** 缓存用户登录信息到redis **/
                Map<String,Object> map = new HashMap<>();
                String str = CommonUtil.getRandomStr(4);
                map.put("admin",adminInfo);
                map.put("str",str);
                RedisUtil.setEx(adminInfo.getAdminId(), JSONObject.fromObject(map).toString(),10800);

                adminInfo.setToken(InterceptorUtil.getToken(adminInfo.getAdminId(),str));

                return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,adminInfo);
            }else{
                return new ResultMessage(ResultMessage.FAILED_CODE,"密码错误");
            }
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"账号错误");
        }
    }

    /**
     * 获取系统操作日志列表
     * @param queryAdminLogSO
     * @return
     */
    public ResultMessage queryAdminLog(QueryAdminLogSO queryAdminLogSO){
        PageHelper.startPage(queryAdminLogSO.getPage(), queryAdminLogSO.getPageSize());
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("adminStr",queryAdminLogSO.getAccount());
        paramMap.put("startTime",queryAdminLogSO.getStartTime());
        paramMap.put("entTime",queryAdminLogSO.getEndTime());

        Page<AdminLog> page =   (Page<AdminLog>)adminLogMapper.selectList(paramMap);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }

}
