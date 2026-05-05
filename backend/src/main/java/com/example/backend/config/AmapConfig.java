package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 高德地图服务配置
 * <p>
 * 通过 amap 前缀读取配置，支持环境变量覆盖：
 * AMAP_WEB_SERVICE_KEY / AMAP_BASE_URL / AMAP_TIMEOUT_MS / AMAP_ENABLED
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "amap")
public class AmapConfig {

    /**
     * 高德 Web 服务 Key（服务端调用使用）
     */
    private String webServiceKey;

    /**
     * 高德 API 基础地址
     */
    private String baseUrl = "https://restapi.amap.com";

    /**
     * HTTP 请求超时时间，单位毫秒
     */
    private int timeoutMs = 5000;

    /**
     * 是否启用高德地图服务，设为 false 时所有调用直接抛异常
     */
    private boolean enabled = true;
}
