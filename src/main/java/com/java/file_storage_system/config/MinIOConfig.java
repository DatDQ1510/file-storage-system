package com.java.file_storage_system.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class MinIOConfig {

    private final MinioProperties minioProperties;

    @Bean
    public OkHttpClient minioOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(minioProperties.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(minioProperties.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(minioProperties.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
                .callTimeout(minioProperties.getCallTimeoutSeconds(), TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(
                        minioProperties.getMaxIdleConnections(),
                        minioProperties.getKeepAliveMinutes(),
                        TimeUnit.MINUTES
                ))
                .retryOnConnectionFailure(true)
                .build();
    }

    @Bean
    public MinioClient minioClient(OkHttpClient minioOkHttpClient) {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .httpClient(minioOkHttpClient)
                .build();
    }
}