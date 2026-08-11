package com.core.manycloudservice.filter;

import com.core.manycloudcommon.entity.SpecialUserKey;
import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.mapper.SpecialUserKeyMapper;
import com.core.manycloudcommon.mapper.UserInfoMapper;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * /api/** 开放接口鉴权拦截器
 * 通过请求头 X-Private-Key 校验特殊用户(user.type=4)，通过后把 userId 放到 request 属性 specialUserId 上
 */
@Slf4j
@Component
public class SpecialKeyInterceptor implements HandlerInterceptor {

    /** 特殊用户类型(type=4) */
    public static final int SPECIAL_USER_TYPE = 4;

    /** 私钥请求头名 */
    public static final String HEADER_PRIVATE_KEY = "X-Private-Key";

    /** 放进 request 属性的 userId 键 */
    public static final String ATTR_SPECIAL_USER_ID = "specialUserId";

    @Autowired
    private SpecialUserKeyMapper specialUserKeyMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception {

        String privateKey = request.getHeader(HEADER_PRIVATE_KEY);

        if(StringUtils.isEmpty(privateKey)){
            return reject(response, "missing_private_key");
        }

        // 查启用中的私钥记录(status=0)
        SpecialUserKey key = specialUserKeyMapper.selectByPrivateKey(privateKey);
        if(key == null){
            return reject(response, "invalid_private_key");
        }

        // 校验对应用户存在且为特殊用户(type=4)
        UserInfo userInfo = userInfoMapper.selectById(key.getUserId());
        if(userInfo == null || userInfo.getType() == null || userInfo.getType() != SPECIAL_USER_TYPE){
            return reject(response, "invalid_private_key");
        }

        // 通过：把 userId 放到 request 上，controller 取用
        request.setAttribute(ATTR_SPECIAL_USER_ID, userInfo.getUserId());
        return true;
    }

    /**
     * 统一返回鉴权失败
     */
    private boolean reject(HttpServletResponse response, String msg) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        ServletOutputStream out = response.getOutputStream();
        ResultMessage result = new ResultMessage("2011", msg);
        out.print(JSONObject.fromObject(result).toString());
        return false;
    }
}
