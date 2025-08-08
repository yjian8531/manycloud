package com.core.manycloudtimer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@MapperScan("com.core.**.mapper.**")
public class ManycloudTimerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManycloudTimerApplication.class, args);
    }

}
