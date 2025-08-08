package com.core.manycloudcommon.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

@Slf4j
public class RedisProperties {

    private static Properties props;
    static {
        String fileName = "redis.properties";
        props = new Properties();
        try {
            props.load(new InputStreamReader(RedisProperties.class.getClassLoader().getResourceAsStream(fileName),"UTF-8"));
        } catch (IOException e) {
            log.error("配置文件redis读取异常",e);
        }
    }
    public static String getProperty(String key){
        String value = props.getProperty(key.trim());
        if(StringUtils.isEmpty(value)){
            return null;
        }
        return value.trim();
    }

    public static String getProperty(String key,String defaultValue){

        String value = props.getProperty(key.trim());
        if(StringUtils.isEmpty(value)){
            value = defaultValue;
        }
        return value.trim();
    }
}
