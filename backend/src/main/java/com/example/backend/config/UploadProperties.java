package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 文件上传配置属性类
 * <p>
 * 读取 application.yml 中 upload.* 配置项，提供文件上传相关参数。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "upload")
public class UploadProperties {

    /** 本地文件保存目录，相对于项目运行目录或绝对路径 */
    private String localDir = "uploads";

    /** 公开访问 URL 前缀，例如 /uploads */
    private String publicPrefix = "/uploads";

    /** 单文件最大大小，单位 MB */
    private int maxSizeMb = 5;

    /** 允许的文件后缀列表，例如 ["jpg", "jpeg", "png"] */
    private List<String> allowedTypes = List.of("jpg", "jpeg", "png", "webp", "pdf");

    /**
     * 获取允许的文件后缀列表（不可变列表，在启动后缓存）
     *
     * @return 后缀列表，不含点号，均为小写
     */
    public List<String> getAllowedTypeList() {
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return allowedTypes;
    }

    /**
     * 获取最大文件大小，单位字节
     *
     * @return 字节数
     */
    public long getMaxSizeBytes() {
        return (long) maxSizeMb * 1024 * 1024;
    }
}
