package com.hl.hlpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.hl.hlpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class HlPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HlPictureBackendApplication.class, args);
    }

}
