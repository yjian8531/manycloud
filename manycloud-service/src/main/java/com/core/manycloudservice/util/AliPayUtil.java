package com.core.manycloudservice.util;

import net.sf.json.JSONObject;

import java.io.InputStream;

public class AliPayUtil {


    /**
     * 获取支付宝私钥
     * @return
     */
    public static String getPrivateKey(){
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("alipay/bly_private_key.txt");
        byte[] bytes = new byte[0];
        try{
            bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
        }catch (Exception e){
            e.printStackTrace();
        }
        return new String(bytes);
    }

    /**
     * 获取支付宝公钥
     * @return
     */
    public static String getPublicKey(){
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("alipay/ply_public_key.txt");
        byte[] bytes = new byte[0];
        try{
            bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
        }catch (Exception e){
            e.printStackTrace();
        }
        return new String(bytes);
    }

    /**
     * app支付状态解析
     * @param code
     * @return
     */
    public static String appPayTradeStatus(String code){
        String result = null;
        switch (code){
            case ("WAIT_BUYER_PAY"):
                result = "交易创建，等待买家付款";
            case ("TRADE_CLOSED"):
                result = "未付款交易超时关闭，或支付完成后全额退款";
            case ("TRADE_SUCCESS"):
                result = "交易支付成功";
            case ("TRADE_FINISHED"):
                result = "交易结束，不可退款";

        }
        return result;
    }

    public static JSONObject analysis(String str){

        JSONObject result = new JSONObject();

        String[] array = str.split("&");
        for(int i=0 ; i < array.length ; i++){
            String[] array2 = array[i].split("=");
            result.put(array2[0],array2[1]);
        }
        return result;
    }

}
