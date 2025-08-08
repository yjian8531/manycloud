package com.core.manycloudcommon.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtil {

    /**
     * 保存文件
     * @param fileName
     * @param content
     * @return
     */
    public static String saveFile(String fileName,String content){
        //生成路径
        // 格式化并获取当前日期（用来命名）
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String path =  "/app/upload/file" ;
        File file = new File(path); // 找到File类的实例
        if (!file.exists() && !file.isDirectory()) {
            file.mkdirs();
        }
        try {
            // 创建文件
            File keyFile = new File(path + "/" + fileName);
            // 声明字符输出流
            Writer out = null;
            // 通过子类实例化，表示可以追加
            out = new FileWriter(keyFile,true);
            // 写入数据
            out.write(content);
            // 保存数据
            out.close();
            return "/file/"+fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

}
