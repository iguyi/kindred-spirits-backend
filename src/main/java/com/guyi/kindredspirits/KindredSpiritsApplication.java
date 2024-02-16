package com.guyi.kindredspirits;

import com.guyi.kindredspirits.common.ProjectProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类
 *
 * @author 孤诣
 */
@SpringBootApplication
@MapperScan("com.guyi.kindredspirits.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableConfigurationProperties(ProjectProperties.class)
public class KindredSpiritsApplication {

    public static void main(String[] args) {
        SpringApplication.run(KindredSpiritsApplication.class, args);
    }

}