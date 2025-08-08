package com.core.manycloudservice.filter;

import com.core.manycloudcommon.entity.SysOperation;
import com.core.manycloudcommon.entity.UserLog;
import com.core.manycloudcommon.mapper.SysOperationMapper;
import com.core.manycloudcommon.mapper.UserLogMapper;
import com.core.manycloudcommon.utils.InterceptorUtil;
import com.core.manycloudcommon.utils.RedisUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.utils.StringUtils;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;


/**
 * 网关拦截器（请求前）
 */
@Slf4j
@Component
public class UserInterceptor implements HandlerInterceptor {

    @Autowired
    private UserLogMapper userLogMapper;

    @Autowired
    private SysOperationMapper sysOperationMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception {

        String roleToken = request.getHeader("h-user-token");
        //请求地址
        String url = request.getRequestURI();
        RequestWrapper requestWrapper = new RequestWrapper(request);

        if(StringUtils.isNotEmpty(roleToken)){
            String src = InterceptorUtil.analysisToken(roleToken);
            if(src == null){
                ServletOutputStream out = response.getOutputStream();
                ResultMessage result = new ResultMessage("2011","invalid_hash_value");
                out.print(JSONObject.fromObject(result).toString());
                return false;
            }else{
                String[] tokens = src.split(":");
                String jsonStr = RedisUtil.get(tokens[0]);
                if(StringUtils.isNotEmpty(jsonStr)){
                    JSONObject json = JSONObject.fromObject(jsonStr);
                    String str = json.getString("str");
                    if(str.equals(tokens[1])){
                        //获取系统记录操作地址信息
                        SysOperation sysOperation = sysOperationMapper.selectByTail(url);
                        String paramStr = requestWrapper.getBodyString();

                        request.setAttribute("userInfo",json.getString("userInfo"));

                        if(sysOperation != null && sysOperation.getType() == 1){//判断是否需要进行重复操作限制
                            String key = json.getJSONObject("userInfo").getString("account")+":"+url+":"+paramStr;
                            if("Y".equals(RedisUtil.get(Base64.encode(key.getBytes())))){//重复请求限制
                                ServletOutputStream out = response.getOutputStream();
                                ResultMessage result = new ResultMessage("1011","Do not repeat the operation");
                                out.print(JSONObject.fromObject(result).toString());
                                return false;
                            }else{
                                RedisUtil.setEx(Base64.encode(key.getBytes()),"Y",2);

                                if(sysOperation != null){
                                    UserLog userLog = new UserLog();
                                    userLog.setUserId(json.getJSONObject("userInfo").getString("userId"));
                                    userLog.setTail(url);
                                    userLog.setAlias(sysOperation.getAlias());
                                    userLog.setIpAddress(getIp(request));
                                    userLog.setRemark(paramStr);
                                    userLog.setCreateTime(new Date());
                                    userLogMapper.insertSelective(userLog);
                                }else{
                                    log.info("User 接口地址:{} ------未被记录-----------",url);
                                }
                            }
                        }

                        return true;
                    }else{
                        //response.setStatus(3333);
                        ServletOutputStream out = response.getOutputStream();
                        ResultMessage result = new ResultMessage("2011","invalid_hash_value");
                        out.print(JSONObject.fromObject(result).toString());
                        return false;
                    }
                }else{
                    //response.setStatus(3333);
                    ServletOutputStream out = response.getOutputStream();
                    ResultMessage result = new ResultMessage("2011","invalid_hash_value");
                    out.print(JSONObject.fromObject(result).toString());
                    return false;
                }
            }

        }
        //response.setStatus(3333);
        ServletOutputStream out = response.getOutputStream();
        ResultMessage result = new ResultMessage("2011","invalid_hash_value");
        out.print(JSONObject.fromObject(result).toString());
        return false;


    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) throws Exception {

    }

    private String getIp(HttpServletRequest request){
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
