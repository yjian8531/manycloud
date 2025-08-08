package com.core.manycloudcommon.utils;

public class InterceptorUtil {

    private static String cKey = "E801491D65C44A76";

    /**
     * 获取用户登录token
     * @param userId 用户ID
     * @param random 随机字符串
     * @return
     */
    public static String getToken(String userId,String random){
        try{
            String token = AESUtil.Encrypt(userId+":"+random,cKey);
            return token;
        }catch (Exception e){
            return null;
        }

    }

    /**
     * 解析用户登录token
     * @param token
     * @return
     */
    public static String analysisToken(String token){
        try{
            String str = AESUtil.Decrypt(token,cKey);
            return str;
        }catch (Exception e){
            return null;
        }
    }

}
