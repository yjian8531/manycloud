package com.core.manycloudcommon.utils;

import com.jcraft.jsch.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
public class JSchConnectUtils {
    private static final String HOST = "你要远程的主机IP地址";
    private static final int PORT = 22;
    private static final String USER = "你要登录的服务器的用户名";
    private static final String PWD = "你要登录的服务器的用户的密码";
    // 超时时间
    private static final int TIMEOUT = 60000000;

    private static Session session;

    /**
     * 链接远程linux服务器
     * @param address
     * @param port
     * @param account
     * @param pwd
     * @return
     * @throws JSchException
     */
    public static Session connect(String address,int port,String account,String pwd) throws JSchException{
        // 创建JSch对象
        JSch jsch = new JSch();
        // 根据用户名，主机ip，端口获取一个Session对象
        Session session = jsch.getSession(account, address, port);
        // 设置密码
        session.setPassword(pwd);
        // 设置timeout时间
        session.setTimeout(60000000);
        // 为Session对象设置properties
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setServerAliveCountMax(3);
        // 通过Session建立链接
        session.connect();

        return session;
    }

    /**
     * 方法描述: 连接到服务器
     * @author yanglichen
     * @date 2022-10-10 20:31
     */
    public static void connect() throws JSchException {
        // 创建JSch对象
        JSch jsch = new JSch();
        // 根据用户名，主机ip，端口获取一个Session对象
        session = jsch.getSession(USER, HOST, PORT);
        // 设置密码
        session.setPassword(PWD);
        // 设置timeout时间
        session.setTimeout(TIMEOUT);
        // 为Session对象设置properties
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        // 通过Session建立链接
        session.connect();
    }

    /**
     * 执行命令
     * @param session 连接对象
     * @param command 命令
     * @return
     * @throws Exception
     */
    public static String execCommand(Session session,String command) throws Exception{
        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        channelExec.setCommand(command);
        channelExec.setInputStream(null);
        channelExec.setErrStream(System.err);
        channelExec.connect();
        InputStream in = channelExec.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        StringBuilder stringBuilder = new StringBuilder();
        String buf = null;
        while ((buf = reader.readLine()) != null) {
            //System.out.println(buf);// 打印控制台输出
            stringBuilder.append(buf).append("\r\n");
        }
        reader.close();
        channelExec.disconnect();

        return stringBuilder.toString();
    }

    /**
     * 上传文件
     * @param session
     * @param localFile
     * @param remoteDir
     * @throws Exception
     */
    public static void fileUpload(Session session,String localFile ,String remoteDir )throws Exception{
        try {
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftp = (ChannelSftp) channel;
            File file = new File(localFile);
            sftp.put(new FileInputStream(file), remoteDir);
            System.out.println("File uploaded successfully!");
            sftp.disconnect();
            channel.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 方法描述: 执行命令
     * @author ylc
     * @date 2022-10-10 16:15
     * @param command 命令
     */
    public static void execCommand(String command) throws Exception {
        System.out.println("正在执行:\t" + command);
        connect();
        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        channelExec.setCommand(command);
        channelExec.setInputStream(null);
        channelExec.setErrStream(System.err);
        channelExec.connect();
        InputStream in = channelExec.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String buf = null;
        while ((buf = reader.readLine()) != null) {
            System.out.println(buf);// 打印控制台输出
        }
        reader.close();
        channelExec.disconnect();
        session.disconnect();
        System.err.println("已执行!\t" + command );
    }

    /**
     * 读取服务器文件内容
     * @param session
     * @param filePath
     * @return
     * @throws Exception
     */
    public static String readFileContent(Session session,String filePath) throws  Exception{
        // 打开SFTP通道
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        // 建立SFTP通道的连接
        channelSftp.connect();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        channelSftp.get(filePath, outputStream);
        String content = new String(outputStream.toByteArray());
        // 关闭通道
        channelSftp.disconnect();
        return content;
    }

    /**
     * 方法描述: 根据远程路径读取文件内容
     * @author ylc
     * @date 2022-10-10 16:35
     * @param absFilePath 远程文件的文件地址
     */
    @SneakyThrows
    public static String readFile(String absFilePath){
        System.out.println("正在读取:\t" + absFilePath);
        connect();
        // 打开SFTP通道
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        // 建立SFTP通道的连接
        channelSftp.connect();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        channelSftp.get(absFilePath, outputStream);
        String fileInfo = new String(outputStream.toByteArray());
        // 关闭通道
        channelSftp.disconnect();
        session.disconnect();
        return fileInfo;
    }

    /**
     * 写入服务器文件内容
     * @param session 连接对象
     * @param filePath 文件路径
     * @param content 内容
     * @throws Exception
     */
    public static void writeFileContent(Session session,String filePath,String content) throws Exception{
        // 打开SFTP通道
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        // 建立SFTP通道的连接
        channelSftp.connect();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        channelSftp.put(inputStream, filePath);
        // 关闭通道
        channelSftp.disconnect();
    }

    /**
     * 方法描述: 将文件内容写入远程路径中
     * @author ylc
     * @date 2022-10-10 16:40
     * @param absFilePath 远程文件的地址
     * @param fileInfo 文件内容
     */
    @SneakyThrows
    public static void writeFile( String fileInfo, String absFilePath){
        System.out.println("正在修改:\t" + absFilePath);
        connect();
        // 打开SFTP通道
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        // 建立SFTP通道的连接
        channelSftp.connect();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileInfo.getBytes());
        channelSftp.put(inputStream, absFilePath);
        // 关闭通道
        channelSftp.disconnect();
        session.disconnect();
    }
}
