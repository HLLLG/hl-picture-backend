package com.hl.hlpicture;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@MapperScan("com.hl.hlpicture.infrastructure.mapper")
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true)
public class HlPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HlPictureBackendApplication.class, args);
    }

}
