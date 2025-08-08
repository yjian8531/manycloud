package com.core.manycloudservice.util;

import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import net.sf.json.JSONObject;
import org.apache.http.impl.client.CloseableHttpClient;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeChatUtil {


    public static CloseableHttpClient createHttpClient(String merchantId,String merchantSerialNumber,String key)throws Exception{
        PrivateKey merchantPrivateKey = PemUtil.loadPrivateKey(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("wechatpay/apiclient_key.pem"));

        // 获取证书管理器实例
        CertificatesManager certificatesManager = CertificatesManager.getInstance();
        // 向证书管理器增加需要自动更新平台证书的商户信息
        certificatesManager.putMerchant(merchantId, new WechatPay2Credentials(merchantId,
                new PrivateKeySigner(merchantSerialNumber, merchantPrivateKey)), key.getBytes(StandardCharsets.UTF_8));

        Verifier verifier = certificatesManager.getVerifier(merchantId);
        X509Certificate credential = verifier.getValidCertificate();

        List<X509Certificate> credentials = new ArrayList<>();
        credentials.add(credential);
        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                .withMerchant(merchantId, merchantSerialNumber, merchantPrivateKey)
                .withWechatPay(credentials);

        // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签
        CloseableHttpClient httpClient = builder.build();
        return httpClient;
    }

    public static Verifier getVerifier(String merchantId,String merchantSerialNumber,String key)throws Exception{
        PrivateKey merchantPrivateKey = PemUtil.loadPrivateKey(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("wechatpay/apiclient_key.pem"));
        // 获取证书管理器实例
        CertificatesManager certificatesManager = CertificatesManager.getInstance();
        // 向证书管理器增加需要自动更新平台证书的商户信息
        certificatesManager.putMerchant(merchantId, new WechatPay2Credentials(merchantId,
                new PrivateKeySigner(merchantSerialNumber, merchantPrivateKey)), key.getBytes(StandardCharsets.UTF_8));

        Verifier verifier = certificatesManager.getVerifier(merchantId);
        return verifier;
    }

    public static String decryptBody(String body,String key) throws UnsupportedEncodingException, GeneralSecurityException {

        AesUtil aesUtil = new AesUtil(key.getBytes("utf-8"));

        JSONObject object = JSONObject.fromObject(body);
        JSONObject resource = object.getJSONObject("resource");
        String ciphertext = resource.getString("ciphertext");
        String associatedData = resource.getString("associated_data");
        String nonce = resource.getString("nonce");

        return aesUtil.decryptToString(associatedData.getBytes("utf-8"),nonce.getBytes("utf-8"),ciphertext);

    }

    /**
     * 转换body为map
     * @param plainBody
     * @return
     */
    public static Map<String,String> convertWechatPayMsgToMap(String plainBody){

        Map<String,String> paramsMap = new HashMap<>(2);

        JSONObject jsonObject = JSONObject.fromObject(plainBody);

        //商户订单号
        paramsMap.put("out_trade_no",jsonObject.getString("out_trade_no"));

        //交易状态
        paramsMap.put("trade_state",jsonObject.getString("trade_state"));

        //附加数据
        //paramsMap.put("account_no",jsonObject.getJSONObject("attach").getString("accountNo"));

        return paramsMap;

    }

    /**
     * 获取用户请求ip
     * @param request
     * @return
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                //根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                ipAddress = inet.getHostAddress();
            }
        }
        //对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ipAddress != null && ipAddress.length() > 15) { //"***.***.***.***".length() = 15
            if (ipAddress.indexOf(",") > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        }
        return ipAddress;
    }


}
