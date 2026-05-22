package com.example.demo.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置类
 */
@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        log.info("MinIO Client initialized, endpoint: {}", endpoint);
        return client;
    }

    /**
     * 初始化 Bucket（如果不存在则创建）
     */
    @Bean
    public String initBucket(MinioClient minioClient) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket [{}] created successfully", bucketName);
            } else {
                log.info("Bucket [{}] already exists", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to init bucket [{}]: {}", bucketName, e.getMessage());
        }
        return bucketName;
    }
}
