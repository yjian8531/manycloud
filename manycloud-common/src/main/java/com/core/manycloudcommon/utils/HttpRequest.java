package com.core.manycloudcommon.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpRequest {

    /**
     * 向指定URL发送GET方法的请求
     *            发送请求的URL
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1.txt;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 向指定URL发送GET方法的请求
     *            发送请求的URL
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url,Map<String,String> headerMap) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1.txt;SV1)");

            for(String key : headerMap.keySet()){
                connection.setRequestProperty(key, headerMap.get(key));
            }

            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            /*for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }*/
            // 定义 BufferedReader输入流来读取URL的响应
            String line;
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    public static String sendDelete(String url, Map<String,String> headerMap){
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            HttpURLConnection connection  = (HttpURLConnection)realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1.txt;SV1)");
            connection.setRequestMethod("DELETE");

            for(String key : headerMap.keySet()){
                connection.setRequestProperty(key, headerMap.get(key));
            }

            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            /*for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }*/
            if(connection.getResponseCode() < 300){
                // 定义 BufferedReader输入流来读取URL的响应
                in = new BufferedReader(new InputStreamReader(
                        connection.getInputStream()));
            }else{
                // 定义 BufferedReader输入流来读取URL的响应
                in = new BufferedReader(new InputStreamReader(
                        connection.getErrorStream()));
            }

            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送DELETE请求出现异常！" + e);
            e.printStackTrace();
            throw new ArithmeticException("发送DELETE请求出现异常！");
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url
     *            发送请求的 URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1.txt;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }


    /**
     * @param url    :请求接口
     * @param params :请求参数
     * @return 返回json结果
     */
    public static String postJson2(String url, String params,Map<String,String> headers) throws IOException {
        PrintWriter out = null;

        BufferedReader in = null;

        String result = "";

        try {

            URL realUrl = new URL(url);

// 打开和URL之间的连接

            URLConnection conn = realUrl.openConnection();

// 设置通用的请求属性

            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if(headers != null){
                for(String key : headers.keySet()){
                    conn.setRequestProperty(key, headers.get(key));
                }
            }

            // 发送POST请求必须设置如下两行

            conn.setDoOutput(true);

            conn.setDoInput(true);

            // 获取URLConnection对象对应的输出流

            out = new PrintWriter(conn.getOutputStream());

            // 发送请求参数

            out.print(params);

            // flush输出流的缓冲

            out.flush();

            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(

                    new InputStreamReader(conn.getInputStream()));

            String line;

            while ((line = in.readLine()) != null) {

                result += line;

            }

        } catch (Exception e) {

            System.out.println("发送 POST 请求出现异常！"+e);

            e.printStackTrace();

        }

//使用finally块来关闭输出流、输入流

        finally{

            try{

                if(out!=null){

                    out.close();

                }

                if(in!=null){

                    in.close();

                }

            }

            catch(IOException ex){

                ex.printStackTrace();

            }

        }

        return result;

    }


    /**
     * @param url    :请求接口
     * @param params :请求参数
     * @return 返回json结果
     */
    public static String postJson(String url, String params,Map<String,String> headers) throws IOException {
        PrintWriter out = null;

        BufferedReader in = null;

        String result = "";

        try {
            URL postUrl = new URL(url);
            // 打开连接
            postUrl.openConnection();
            URL realUrl = new URL(url);

            // 打开和URL之间的连接
            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();


            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if(headers != null){
                for(String key : headers.keySet()){
                    conn.setRequestProperty(key, headers.get(key));
                }
            }


            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // 获取URLConnection对象对应的输出流

            out = new PrintWriter(conn.getOutputStream());

            // 发送请求参数
            out.print(params);

            // flush输出流的缓冲
            out.flush();
            System.out.println("请求结果："+conn.getResponseCode());
            if(conn.getResponseCode() < 300){
                // 定义BufferedReader输入流来读取URL的响应
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            }else{
                // 定义BufferedReader输入流来读取URL的响应
                in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }


            String line;

            while ((line = in.readLine()) != null) {
                result += line;
            }

        } catch (Exception e) {

            System.out.println("发送 POST 请求出现异常！"+e);

            e.printStackTrace();

        }finally{//使用finally块来关闭输出流、输入流

            try{

                if(out!=null){

                    out.close();

                }

                if(in!=null){

                    in.close();

                }

            }

            catch(IOException ex){

                ex.printStackTrace();

            }

        }

        return result;

    }

    /**
     * @param url    :请求接口
     * @return 返回json结果
     */
    public static String get(String url,Map<String,String> headers) throws IOException {
        URL postUrl = new URL(url);
        // 打开连接
        HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
        // 设置是否向connection输出，因为这个是post请求，参数要放在
        // http正文内，因此需要设为true
        connection.setDoOutput(true);
        connection.setDoInput(true);
        // 默认是 GET方式
        //connection.setRequestMethod("POST");

        // Post 请求不能使用缓存
        connection.setUseCaches(false);
        //设置本次连接是否自动重定向
        connection.setInstanceFollowRedirects(true);

        connection.setRequestProperty("accept", "*/*");
        connection.setRequestProperty("connection", "Keep-Alive");
        connection.setRequestProperty("user-agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
        for(String key : headers.keySet()){
            connection.setRequestProperty(key, headers.get(key));
        }
        connection.connect();

        InputStreamReader inputStreamReader = null;
        if(connection.getResponseCode() < 300){
            inputStreamReader = new InputStreamReader(connection.getInputStream());
        }else{
            inputStreamReader = new InputStreamReader(connection.getErrorStream());
        }
        //获取响应
        BufferedReader reader = new BufferedReader(inputStreamReader);
        StringBuffer sb = new StringBuffer();
        String line = "";
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        //该干的都干完了,记得把连接断了
        connection.disconnect();
        return sb.toString();
    }


    /**
     * @param url    :请求接口
     * @param params :请求参数
     * @return 返回json结果
     */
    public static String post(String url, Map<String, String> params,Map<String,String> headers) throws IOException {
        URL postUrl = new URL(url);
        // 打开连接
        HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
        // 设置是否向connection输出，因为这个是post请求，参数要放在
        // http正文内，因此需要设为true
        connection.setDoOutput(true);
        connection.setDoInput(true);
        // 默认是 GET方式
        connection.setRequestMethod("POST");

        // Post 请求不能使用缓存
        connection.setUseCaches(false);
        //设置本次连接是否自动重定向
        connection.setInstanceFollowRedirects(true);
        // 配置本次连接的Content-type，配置为application/x-www-form-urlencoded的
        // 意思是正文是urlencoded编码过的form参数
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        for(String key : headers.keySet()){
            connection.setRequestProperty(key, headers.get(key));
        }
        connection.connect();
        DataOutputStream out = new DataOutputStream(connection
                .getOutputStream());
        // 正文，正文内容其实跟get的URL中 '? '后的参数字符串一致
        String content = mp2String(params);
        content =  content.substring(1,content.length());
        // DataOutputStream.writeBytes将字符串中的16位的unicode字符以8位的字符形式写到流里面
        out.writeBytes(content);
        //流用完记得关
        out.flush();
        out.close();
        //获取响应
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuffer sb = new StringBuffer();
        String line = "";
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        //该干的都干完了,记得把连接断了
        connection.disconnect();
        return sb.toString();
    }


    /**
     * @param url    :请求接口
     * @param params :请求参数
     * @return 返回json结果
     */
    public static String post(String url, Map<String, String> params) throws IOException {
        URL postUrl = new URL(url);
        // 打开连接
        HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
        // 设置是否向connection输出，因为这个是post请求，参数要放在
        // http正文内，因此需要设为true
        connection.setDoOutput(true);
        connection.setDoInput(true);
        // 默认是 GET方式
        connection.setRequestMethod("POST");

        // Post 请求不能使用缓存
        connection.setUseCaches(false);
        //设置本次连接是否自动重定向
        connection.setInstanceFollowRedirects(true);
        // 配置本次连接的Content-type，配置为application/x-www-form-urlencoded的
        // 意思是正文是urlencoded编码过的form参数
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.connect();
        DataOutputStream out = new DataOutputStream(connection
                .getOutputStream());
        // 正文，正文内容其实跟get的URL中 '? '后的参数字符串一致
        String content = mp2String(params);
        content =  content.substring(1,content.length());
        // DataOutputStream.writeBytes将字符串中的16位的unicode字符以8位的字符形式写到流里面
        out.writeBytes(content);
        //流用完记得关
        out.flush();
        out.close();

        int code = connection.getResponseCode();
        StringBuffer sb = new StringBuffer();
        if(code == 200){
            //获取响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            //该干的都干完了,记得把连接断了
        }else{
            System.out.println("响应状态异常："+code);
            // 读取响应内容
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }
            in.close();
        }

        connection.disconnect();
        return sb.toString();
    }

    private static String mp2String(Map<String, String> maps) throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        Set<Map.Entry<String, String>> entries = maps.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            sb.append("&"+entry.getKey()+"="+ URLEncoder.encode(entry.getValue(),"utf-8"));
        }
        return sb.toString();
    }

    private static String jsonString(Map<String, String> maps) throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        Set<Map.Entry<String, String>> entries = maps.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            sb.append("&"+entry.getKey()+"="+ URLEncoder.encode(entry.getValue(),"utf-8"));
        }
        return sb.toString();
    }




}
