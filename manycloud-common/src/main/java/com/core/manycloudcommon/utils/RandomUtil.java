package com.core.manycloudcommon.utils;

import java.util.Random;

/**
 *
 * 生成带大小写字母及数字的随机字符串
 *
 * @author Jack_David
 *
 * @since 1.0.0
 *
 * @Date 2019-05-05
 *
 */
public class RandomUtil {

    /**
     * 字符串池
     */
    private static String[] STR_ARR = new String[] { "a", "b", "c", "d", "e",
            "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r",
            "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E",
            "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
            "S", "T", "U", "V", "W", "X", "Y", "Z", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "0" };

    /**
     * 字符串池
     */
    private static String[] STR_ARR2 = new String[] { "a", "b", "c", "d", "e",
            "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r",
            "s", "t", "u", "v", "w", "x", "y", "z" };

    /**
     * 字符串池
     */
    private static String[] STR_ARR3 = new String[] { "A", "B", "C", "D", "E",
            "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
            "S", "T", "U", "V", "W", "X", "Y", "Z" };

    /**
     * 字符串池
     */
    private static String[] STR_ARR4 = new String[] { "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "0" };

    public static String getRjPwd() {
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < 12; i++) {
            if(i == 2){
                sb.append(STR_ARR4[rand.nextInt(STR_ARR4.length)]);
            }else if(i == 7){
                sb.append(STR_ARR4[rand.nextInt(STR_ARR4.length)]);
            }else{
                sb.append(STR_ARR[rand.nextInt(STR_ARR.length)]);
            }

        }
        return sb.toString();
    }

    /**
     * 根据指定的长度生成的含有大小写字母及数字的字符串
     * @param length 指定的长度
     * @return 按照指定的长度生成的含有大小写字母及数字的字符串
     */
    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(STR_ARR[rand.nextInt(STR_ARR.length)]);
        }
        return sb.toString();
    }


}
