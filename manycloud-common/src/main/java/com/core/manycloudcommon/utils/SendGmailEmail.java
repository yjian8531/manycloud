package com.core.manycloudcommon.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class SendGmailEmail {

    public static void sendVerificationCode(String toEmail,String title, String content) {
        // 设置邮件服务器属性
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        // 邮箱账号和应用程序密码（注意：要使用Gmail，需要生成应用专用密码）
        String myAccountEmail = "luoteyun@gmail.com";
        String password = "njrcujqncvjghgvh"; // 生成的应用专用密码

        // 获取 Session
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(myAccountEmail, password);
            }
        });

        // 创建邮件内容
        Message message = prepareMessage(session, myAccountEmail, toEmail, title,content);

        // 发送邮件
        try {
            Transport.send(message);
            System.out.println("验证码已发送！");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    // 构建邮件内容的方法
    private static Message prepareMessage(Session session, String myAccountEmail, String recipientEmail,String title, String content) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(myAccountEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject(title);
            message.setText(content);
            return message;
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        // 示例调用
        String recipientEmail = "503141708@qq.com";
        String emailCode = "567438"; // 生成的验证码
        sendVerificationCode(recipientEmail,"【洛特云】注册验证码","验证码:"+emailCode+"，您正在注册成为新用户，感谢您的支持！");
    }

}
