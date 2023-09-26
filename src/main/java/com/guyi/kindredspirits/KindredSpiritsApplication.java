package com.guyi.kindredspirits;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 */
@SpringBootApplication
@MapperScan("com.guyi.kindredspirits.mapper")
public class KindredSpiritsApplication {

    public static void main(String[] args) {
        SpringApplication.run(KindredSpiritsApplication.class, args);
    }

}