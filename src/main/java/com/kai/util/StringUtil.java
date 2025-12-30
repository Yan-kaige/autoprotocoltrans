package com.kai.util;

import java.util.Random;

/**
 * 字符串工具类
 * 提供各种字符串处理方法，可用于自定义后端方法转换
 */
public class StringUtil {
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random random = new Random();
    
    /**
     * 生成随机8位字符串
     * 包含大小写字母和数字
     * 
     * @param input 输入参数（此方法不使用输入值，但为了符合转换策略接口要求保留此参数）
     * @return 随机8位字符串
     */
    public static Object generateRandom8Chars(Object input) {
        return generateRandomString(8);
    }
    
    /**
     * 生成指定长度的随机字符串
     * 包含大小写字母和数字
     * 
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String generateRandomString(int length) {
        if (length <= 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
    
    /**
     * 生成随机8位数字字符串
     * 只包含数字 0-9
     * 
     * @param input 输入参数（此方法不使用输入值，但为了符合转换策略接口要求保留此参数）
     * @return 随机8位数字字符串
     */
    public static Object generateRandom8Digits(Object input) {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    /**
     * 转大写
     * 
     * @param input 输入字符串
     * @return 转大写后的字符串
     */
    public static Object toUpperCase(Object input) {
        if (input == null) {
            return null;
        }
        return input.toString().toUpperCase();
    }
    
    /**
     * 转小写
     * 
     * @param input 输入字符串
     * @return 转小写后的字符串
     */
    public static Object toLowerCase(Object input) {
        if (input == null) {
            return null;
        }
        return input.toString().toLowerCase();
    }
    
    /**
     * 去除首尾空格
     * 
     * @param input 输入字符串
     * @return 去除空格后的字符串
     */
    public static Object trim(Object input) {
        if (input == null) {
            return null;
        }
        return input.toString().trim();
    }
}

