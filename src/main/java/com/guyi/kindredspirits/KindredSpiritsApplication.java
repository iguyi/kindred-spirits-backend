package com.guyi.kindredspirits;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类
 * @author 张仕恒
 */
@SpringBootApplication
@MapperScan("com.guyi.kindredspirits.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
public class KindredSpiritsApplication {

    public static void main(String[] args) {
        SpringApplication.run(KindredSpiritsApplication.class, args);
    }

}