package com.core.manycloudcommon.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

public class SendWYEmail {

    /**
     *
     * 配置发送基本参数
     * 发件人邮箱的SMTP服务器地址
     * 前三个不可更改
     *
     * */
    private final static String MyEmail = "liebaotk@163.com";//开启授权码的邮箱
    private final static String AuthorizationCode = "NHIJCFEWSMPQRVBH";//授权码
    private final static String SMTPEmail = "smtp.office365.com";// outlook邮箱的 SMTP 服务器地址


    public static void main(String[] args) throws Exception {
        //创建连接邮件服务器的参数配置
        Properties props = new Properties();// 参数配置
        props.setProperty("mail.smtp.host", SMTPEmail);// 发件人的邮箱的 SMTP 服务器地址
        props.setProperty("mail.smtp.auth", "true");// 需要请求认证
        props.setProperty("mail.transport.protocol", "smtp");
        //根据配置创建会话对象和邮件服务器交互
        Session session = Session.getInstance(props);
        session.setDebug(true);// 设置为debug模式, 可以查看详细的发送日志
        //创建邮件
        MimeMessage message = createEmail(session, MyEmail, "503141708@qq.com","注册验证码","您好，您的验证码为 344564。请勿告知他人");
        //使用Session获取邮件传输对象
        Transport transport = session.getTransport();
        //使用邮箱账号和密码连接邮件服务器
        transport.connect(MyEmail, AuthorizationCode);
        //发送邮件
        transport.sendMessage(message, message.getAllRecipients());
        //关闭连接
        transport.close();
    }


    /**
     * 发送邮件
     * @param title
     * @param content
     * @param to
     */
    public static void send(String title,String content,String to){
        //创建连接邮件服务器的参数配置
        Properties props = new Properties();// 参数配置
        // 设置邮件服务器主机名
        props.setProperty("mail.smtp.host","smtp.office365.com");
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.port", "587");

        props.setProperty("mail.smtp.host", SMTPEmail);// 发件人的邮箱的 SMTP 服务器地址
        props.setProperty("mail.smtp.auth", "true");// 需要请求认证
        props.setProperty("mail.transport.protocol", "smtp");
        //根据配置创建会话对象和邮件服务器交互
        Session session = Session.getInstance(props);
        session.setDebug(true);// 设置为debug模式, 可以查看详细的发送日志
        try{
            //创建邮件
            MimeMessage message = createEmail(session, MyEmail, to,title,content);
            //使用Session获取邮件传输对象
            Transport transport = session.getTransport();
            //使用邮箱账号和密码连接邮件服务器
            transport.connect(MyEmail, AuthorizationCode);
            //发送邮件
            transport.sendMessage(message, message.getAllRecipients());
            //关闭连接
            transport.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     *
     * 创建邮件
     *
     * */
    public static MimeMessage createEmail(Session session, String sendMail, String receiveMail,String title,String content) throws Exception {
        //创建一封邮件
        MimeMessage message = new MimeMessage(session);
        //发件人
        message.setFrom(new InternetAddress(sendMail, "猎豹TK", "UTF-8"));
        //收件人
        message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(receiveMail, "XX用户", "UTF-8"));
        //邮件主题
        message.setSubject(title, "UTF-8");
        //邮件正文
        message.setContent(content, "text/html;charset=UTF-8");
        //设置发件时间
        message.setSentDate(new Date());
        //保存设置
        message.saveChanges();
        return message;
    }

}
