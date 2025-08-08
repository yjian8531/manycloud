package com.core.manycloudcommon.controller;

import com.core.manycloudcommon.entity.AdminInfo;
import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.utils.MD5;
import com.core.manycloudcommon.utils.RedisUtil;
import net.sf.json.JSONObject;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class BaseController {

    public HttpServletRequest getRequest() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        return ((ServletRequestAttributes) ra).getRequest();
    }

    public HttpServletResponse getResponse(){
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        return ((ServletRequestAttributes) ra).getResponse();
    }

    public HttpSession getSession(){
        return this.getRequest().getSession();
    }

    public UserInfo getLoginUser(){
        HttpServletRequest request = getRequest();
        Object str = request.getAttribute("userInfo");
        UserInfo userInfo = (UserInfo)JSONObject.toBean(JSONObject.fromObject(str),UserInfo.class);
        return userInfo;
    }

    public AdminInfo getLoginAdmin(){
        HttpServletRequest request = getRequest();
        Object str = request.getAttribute("adminInfo");
        AdminInfo adminInfo = (AdminInfo)JSONObject.toBean(JSONObject.fromObject(str),AdminInfo.class);
        return adminInfo;
    }

    public String getUserId(){
        HttpServletRequest request = getRequest();
        Object str = request.getAttribute("userId");
        return str.toString();
    }

    /**
     * 重复提交限制
     * @param params
     * @return
     */
    public boolean restriction(String... params){
        HttpServletRequest request = getRequest();
        String url = request.getRequestURI();
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < params.length; i++) {
            if(params[i] != null){
                str.append(params[i]);
            }
        }
        String key = MD5.MD5Encode(url+":"+str);
        String value =  RedisUtil.get(key);
        if(value == null){
            RedisUtil.setEx(key,"Y",20);
            return false;
        }else{
            return true;
        }
    }

    public String getIp(){
        HttpServletRequest request = getRequest();
        String ip = request.getHeader("x-forwarded-for");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;

    }

}
