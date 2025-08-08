package com.core.manycloudadmin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@ServletComponentScan
@MapperScan("com.core.**.mapper.**")
public class ManycloudAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManycloudAdminApplication.class, args);
    }

}
