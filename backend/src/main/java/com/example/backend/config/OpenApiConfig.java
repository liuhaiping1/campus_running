package com.example.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI（Swagger）配置类
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置OpenAPI文档信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园万能帮跑腿服务平台 API")
                        .version("1.0.0")
                        .description("基于OpenAPI 3.0规范的校园跑腿服务平台接口文档"));
    }
}
