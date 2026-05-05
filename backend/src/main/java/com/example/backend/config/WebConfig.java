package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web全局配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    /**
     * 构造函数注入上传配置属性
     *
     * @param uploadProperties 上传配置属性
     */
    public WebConfig(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    /**
     * 配置CORS跨域支持
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置文件上传静态资源映射
     * <p>
     * 将 /uploads/** 路径映射到本地上传目录，使上传文件可通过 URL 直接访问。
     * </p>
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + uploadProperties.getLocalDir() + "/";
        registry.addResourceHandler(uploadProperties.getPublicPrefix() + "/**")
                .addResourceLocations(location);
    }
}
