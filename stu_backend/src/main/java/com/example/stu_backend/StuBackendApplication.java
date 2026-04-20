package com.example.stu_backend;

import com.example.stu_backend.util.LoggerUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StuBackendApplication {

    public static void main(String[] args) {
        LoggerUtil.logger.info("stu_backend 启动");
        SpringApplication.run(StuBackendApplication.class, args);
    }

}
