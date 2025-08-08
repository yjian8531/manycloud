package com.core.manycloudadmin.filter;

import com.core.manycloudcommon.entity.AdminLog;
import com.core.manycloudcommon.entity.SysOperation;
import com.core.manycloudcommon.mapper.AdminLogMapper;
import com.core.manycloudcommon.mapper.SysOperationMapper;
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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;


@Slf4j
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Autowired
    private AdminLogMapper adminLogMapper;

    @Autowired
    private SysOperationMapper sysOperationMapper;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception {

        String adminToken = request.getHeader("h-admin-token");

        //请求地址
        String url = request.getRequestURI();
        RequestWrapper requestWrapper = new RequestWrapper(request);

        if(StringUtils.isNotEmpty(adminToken)){
            String src = InterceptorUtil.analysisToken(adminToken);
            if(src == null){
                ServletOutputStream out = response.getOutputStream();
                ResultMessage result = new ResultMessage("3333","invalid_hash_value");
                out.print(JSONObject.fromObject(result).toString());
                return false;
            }else{
                String[] tokens = src.split(":");
                String jsonStr = RedisUtil.get(tokens[0]);
                if(StringUtils.isNotEmpty(jsonStr)){
                    JSONObject json = JSONObject.fromObject(jsonStr);
                    String str = json.getString("str");
                    if(str.equals(tokens[1])){

                        request.setAttribute("adminInfo",json.getString("admin"));
                        //获取系统记录操作地址信息
                        SysOperation sysOperation = sysOperationMapper.selectByTail(url);
                        String paramStr = requestWrapper.getBodyString();

                        if(sysOperation != null && sysOperation.getType() == 1){//判断是否需要进行重复操作限制

                            String key = json.getJSONObject("admin").getString("adminId")+":"+url+":"+paramStr;
                            if("Y".equals(RedisUtil.get(Base64.encode(key.getBytes())))  && url.indexOf("get") == -1 && url.indexOf("query") == -1){
                                response.setContentType("text/html;charset=GBK");
                                ServletOutputStream out = response.getOutputStream();
                                ResultMessage result = new ResultMessage("3333","Do not repeat the operation");
                                out.print(JSONObject.fromObject(result).toString());
                                return false;
                            }else{
                                RedisUtil.setEx(Base64.encode(key.getBytes()),"Y",2);

                                if(sysOperation != null){
                                    AdminLog adminLog = new AdminLog();
                                    adminLog.setAdminId(json.getJSONObject("admin").getString("adminId"));
                                    adminLog.setTail(url);
                                    adminLog.setAlias(sysOperation.getAlias());
                                    adminLog.setRemark(paramStr);
                                    adminLog.setCreateTime(new Date());
                                    adminLogMapper.insertSelective(adminLog);
                                }else{
                                    log.info("admin 接口地址:{} ------未被记录-----------",url);
                                }
                            }
                        }
                        return true;

                    }else{
                        ServletOutputStream out = response.getOutputStream();
                        ResultMessage result = new ResultMessage("3333","invalid_hash_value");
                        out.print(JSONObject.fromObject(result).toString());
                        return false;
                    }
                }else{
                    ServletOutputStream out = response.getOutputStream();
                    ResultMessage result = new ResultMessage("3333","invalid_hash_value");
                    out.print(JSONObject.fromObject(result).toString());
                    return false;
                }
            }

        }
        response.setStatus(3333);
        return false;

    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) throws Exception {

    }

}
