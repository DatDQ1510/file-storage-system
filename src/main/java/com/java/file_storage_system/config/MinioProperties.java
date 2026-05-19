package com.java.file_storage_system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private boolean secure;
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 30;
    private int writeTimeoutSeconds = 30;
    private int callTimeoutSeconds = 60;
    private int maxIdleConnections = 20;
    private int keepAliveMinutes = 5;
}
