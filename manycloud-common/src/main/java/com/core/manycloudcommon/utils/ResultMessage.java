package com.core.manycloudcommon.utils;

public class ResultMessage {

    public static String SUCCEED_CODE = "0000";

    public static String SUCCEED_MSG = "SUCCEED";

    public static String FAILED_CODE = "1111";

    public static String FAILED_MSG = "FAILED";

    public static String RE2011_CODE = "2011";

    public static String RE2011_MSG = "驳回";

    public static String RE2022_CODE = "2022";

    public static String RE2022_MSG = "重定向";

    public static String RE500_CODE = "500";

    public static String RE500_MSG = "服务器繁忙，请稍后再试";

    private String code;

    private String msg;

    private Object data;

    public ResultMessage(String code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public ResultMessage(String code, String msg, Object data){
        this.code = code;
        this.msg = msg;
        this.data = data;
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
