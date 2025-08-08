package com.core.manycloudcommon.utils;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SendQQMailUtil {

    /**
     * QQ邮箱推送
     * @param title 标题
     * @param content 内容
     * @throws AddressException
     * @throws MessagingException
     */
    public static void send(String title,String content,String... toEmails){

        try{

            // 创建Properties 类用于记录邮箱的一些属性
            Properties props = new Properties();
            // 表示SMTP发送邮件，必须进行身份验证
            props.put("mail.smtp.auth", "true");
            //此处填写SMTP服务器
            props.put("mail.smtp.host", "smtp.exmail.qq.com");
            //端口号，QQ邮箱端口587
            props.put("mail.smtp.port", "465");
            // 此处填写，写信人的账号
            props.put("mail.user", "basexk@lotvps.com");
            // 此处填写16位STMP口令
            props.put("mail.password", "LYXCJRPbFtaaia2p");

            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");


            // 构建授权信息，用于进行SMTP进行身份验证
            Authenticator authenticator = new Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {
                    // 用户名、密码
                    String userName = props.getProperty("mail.user");
                    String password = props.getProperty("mail.password");
                    return new PasswordAuthentication(userName, password);
                }
            };
            // 使用环境属性和授权信息，创建邮件会话
            Session mailSession = Session.getInstance(props, authenticator);
            // 创建邮件消息
            MimeMessage message = new MimeMessage(mailSession);
            // 设置发件人
            InternetAddress form = new InternetAddress(props.getProperty("mail.user"));
            message.setFrom(form);

            InternetAddress[] tos = new InternetAddress[toEmails.length];
            List<InternetAddress> list = new ArrayList<>();
            for(int i = 0 ; i < toEmails.length ; i++){
                tos[i] = (new InternetAddress(toEmails[i]));
            }
            // 设置收件人的邮箱
            //InternetAddress to = new InternetAddress(toEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, tos);

            // 设置邮件标题
            message.setSubject(title);

            // 设置邮件的内容体
            message.setContent(content, "text/html;charset=UTF-8");

            // 最后当然就是发送邮件啦
            Transport.send(message);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /*public static void main(String[] args) {
        // 示例调用
        String recipientEmail = "503141708@qq.com";
        String emailCode = "569407"; // 生成的验证码
        send("【洛特云】注册验证码","验证码:"+emailCode+"，您正在注册成为新用户，感谢您的支持！",recipientEmail);
    }*/
}
