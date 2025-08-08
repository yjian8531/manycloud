package com.core.manycloudcommon.utils;

import com.core.manycloudcommon.entity.RegionContinent;
import com.core.manycloudcommon.enums.MainEnum;
import com.core.manycloudcommon.enums.PlatformLabelEnum;
import net.sf.json.JSONObject;
import org.springframework.beans.BeanUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CommonUtil {


    public static int STATUS_0 = 0;
    public static int STATUS_1 = 1;
    public static int STATUS_2 = 2;
    public static int STATUS_3 = 3;
    public static int STATUS_4 = 4;
    public static int STATUS_5 = 5;
    public static int STATUS_6 = 6;
    public static int STATUS_7 = 7;
    public static int STATUS_8 = 8;
    public static int STATUS_9 = 9;
    public static int STATUS_10 = 10;
    public static int STATUS_11 = 11;

    public static String SUCCESS_CODE = "0000";
    public static String FAIL_CODE = "1111";
    public static String SUCCESS_MSG = "SUCCESS";
    public static String FAIL_MSG = "FAIL";


    public static void main(String[] args){
        System.out.println("ID :"+getRandomStr(32));
        System.out.println("密文:"+MD5.MD5Encode(MD5.MD5Encode(MD5.MD5Encode("wef&123"))));
    }

    /**
     * 字符串转码
     * @param str
     * @param code UTF-8
     * @return
     */
    public static String strEncoder(String str,String code){
        Charset charset = Charset.forName(code);
        byte[] bytes = str.getBytes(charset);
        String decodedStr = new String(bytes,charset);
        return decodedStr;
    }

    /**
     * 生产唯一流水号
     * @param mainEnum 流水号类型
     * @return
     */
    public static String getOnlyNo(MainEnum mainEnum){
        String result = mainEnum.getName()+"-"+DateUtil.dateStrYYYYMMDD(new Date())+"-"+getRandomNumber(4);
        return result;
    }

    /***
     * 获取实例密码
     * @param platformLabelEnum
     * @return
     */
    public static String getConnectPwd(PlatformLabelEnum platformLabelEnum){
        String pwd;
        if(PlatformLabelEnum.UCLOUD.equals(platformLabelEnum)){
            pwd = RandomUtil.getRjPwd();
        }else if(PlatformLabelEnum.ALIYUN.equals(platformLabelEnum)){
            int j = CommonUtil.getRandom(12,18);
            pwd = CommonUtil.getPsw(j);
        }else{
            int j = CommonUtil.getRandom(12,18);
            pwd = CommonUtil.getPsw(j);
        }
        return pwd;
    }

    /**
     * 获取推广码
     * @param marketList
     * @return
     */
    public static String getMarket(List<String> marketList){
        String market = null;
        do{
            market = getRandomStr(4);
            if(marketList.contains(market)){
                market = null;
            }
        }while ( market == null);
        return market;
    }

    /**
     * 获取推广码
     * @param type
     * @return
     */
    public static String getDepartmentTypeName(Integer type){
        String name = null;
        if(type == 0){
            name = "渠道部";
        }else if(type == 1){
            name = "销售部";
        }
        return name;
    }



    /**
     * 获取随机字符串
     * @param l 长度
     * @return
     */
    public static String getRandomStr(int l){
        String uuid = UUID.randomUUID().toString().replaceAll("-","");
        while (uuid.length() < l){
            uuid += UUID.randomUUID().toString().replaceAll("-","");
        }
        return uuid.substring(0,l);
    }

    /**
     * 舍弃小数点
     * @param num
     * @return
     */
    public static String removeDecimal(BigDecimal num){
        String numStr = num.toPlainString();
        int i = numStr.lastIndexOf(".");
        if( i < 0 ){
            return numStr;
        }else{
            return numStr.substring(0,i);
        }
    }

    /**
     * 获取随机数
     * @param l 长度
     * @return
     */
    public static String getRandomNumber(int l){
        return StringUtils.buildRandom(l)+"";
    }

    /**
     * JSONObject转Map
     * @param json
     * @return
     */
    public static Map<String,Object> jsonToMap(JSONObject json){
        //map对象
        Map<String, Object> data =new HashMap<>();
        //循环转换
        Iterator it =json.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) it.next();
            data.put(entry.getKey(), entry.getValue());
        }
        return data;
    }

    /**
     * 字符串转Map
     * @param str
     * @return
     */
    public static Map<String,Object> stringToMap(String str){
        //map对象
        Map<String, Object> data =new HashMap<>();
        String[] array1 = str.split(",");
        for(int i =0 ; i<array1.length; i++){
            String[] array2 = array1[i].split(":");
            if(array2.length == 2){
                data.put(array2[0],array2[1]);
            }
        }
        return data;
    }

    public static int getRandom(int min, int max){
        Random random = new Random();
        int s = random.nextInt(max) % (max - min + 1) + min;
        return s;

    }

    public static String getPsw(int len) {
        // 1、定义基本字符串baseStr和出参password
        String password = null;
        String baseStr = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        boolean flag = false;
        // 2、使用循环来判断是否是正确的密码
        while (!flag) {
            // 密码重置
            password = "";
            // 个数计数
            int a = 0,b = 0,c = 0,d = 0;
            for (int i = 0; i < len; i++) {
                int rand = (int) (Math.random() * baseStr.length());
                password+=baseStr.charAt(rand);
                if (0<=rand && rand<=9) {
                    a++;
                }
                if (10<=rand && rand<=35) {
                    b++;
                }
                if (36<=rand && rand<=61) {
                    c++;
                }
                if (62<=rand && rand<baseStr.length()) {
                    d++;
                }
            }
            // 3、判断是否是正确的密码（4类中仅一类为0，其他不为0）
            flag = (a*b*c!=0&&d==0)||(a*b*d!=0&&c==0)||(a*c*d!=0&&b==0)||(b*c*d!=0&&a==0);
        }
        return password;
    }

    public static String mapToStringURLEncoder(Map<String, Object> map)throws Exception {
        StringBuffer sb = new StringBuffer();
        SortedMap<String, Object> params = new TreeMap<>();
        for (String key : map.keySet()) {
            params.put(key,map.get(key));

        }
        for(String key : params.keySet()){
            String str = key + "=" + URLEncoder.encode(params.get(key).toString(), "UTF-8" ) + "&";
            sb.append(str);
        }
        String result = sb.toString().substring(0, sb.length() - 1);
        return result;
    }

    public static String mapToString(Map<String, String> map)throws Exception {
        StringBuffer sb = new StringBuffer();
        SortedMap<String, Object> params = new TreeMap<>();
        for (String key : map.keySet()) {
            params.put(key,map.get(key));

        }
        for(String key : params.keySet()){
            String str = key + "=" + params.get(key) + "&";
            sb.append(str);
        }
        String result = sb.toString().substring(0, sb.length() - 1);
        return result;
    }

    public static boolean isChinese(char c) {
        return String.valueOf(c).matches("[\\u4e00-\\u9fa5]");
    }

    public static String nodeNameRetrieve(String node){
        StringBuffer str = new StringBuffer();
        for (char c : node.toCharArray()) {
            if (isChinese(c)) {
                str.append(c);
            }else{
                break;
            }
        }
        return str.toString();
    }









    /**
     * 获取参数模板
     * @return
     */
    private static String getVmessParamModel(){
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file/urltst.txt");
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
     * 复制对象相同字段
     * @param source 从
     * @param target 到
     */
    public static void copyProperties(Object source, Object target) {
        BeanUtils.copyProperties(source, target);
    }




}
