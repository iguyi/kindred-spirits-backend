package com.guyi.kindredspirits.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * Swagger 配置
 */
@Configuration
@EnableSwagger2WebMvc
@Profile({"dev", "test"})
public class SwaggerConfig {

    @Bean(value = "defaultApi2")
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.guyi.kindredspirits.controller"))  // 重要，标注控制器的位置
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * API 信息
     *
     * @return
     */
    public ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("道友--伙伴匹配系统")  // 设置标题
                .description("")  // 设置文档的描述
                .version("1.0")
                .contact(new Contact("孤诣", "", ""))
                .termsOfServiceUrl("")  // 设置文档的 许可证 信息
                .build();
    }
}