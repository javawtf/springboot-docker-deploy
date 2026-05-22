package com.example.demo.util;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 工具类 — 封装常用文件操作
 */
@Component
public class MinioUtil {

    private static final Logger log = LoggerFactory.getLogger(MinioUtil.class);

    @Resource
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    // ===================== 上传 =====================

    /**
     * MultipartFile 上传
     */
    public String uploadFile(MultipartFile file) throws Exception {
        return uploadFile(file, file.getOriginalFilename());
    }

    /**
     * MultipartFile 上传，指定对象名
     */
    public String uploadFile(MultipartFile file, String objectName) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
        log.info("File uploaded: {}", objectName);
        return objectName;
    }

    /**
     * InputStream 上传
     */
    public String uploadFile(InputStream inputStream, String objectName, String contentType, long size) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build());
        log.info("File uploaded: {}", objectName);
        return objectName;
    }

    // ===================== 下载 =====================

    /**
     * 下载文件输入流
     */
    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
    }

    // ===================== 预签名 URL =====================

    /**
     * 获取预签名访问 URL（默认 7 天有效）
     */
    public String getPresignedUrl(String objectName) throws Exception {
        return getPresignedUrl(objectName, 7, TimeUnit.DAYS);
    }

    /**
     * 获取预签名访问 URL
     */
    public String getPresignedUrl(String objectName, int duration, TimeUnit unit) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(duration, unit)
                        .build());
    }

    // ===================== 删除 =====================

    /**
     * 删除文件
     */
    public void removeFile(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
        log.info("File removed: {}", objectName);
    }

    // ===================== 判断文件是否存在 =====================

    /**
     * 判断文件是否存在
     */
    public boolean fileExists(String objectName) throws Exception {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
