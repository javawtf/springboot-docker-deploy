package com.example.demo.controller;

import com.example.demo.util.MinioUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * MinIO 文件管理接口
 */
@RestController
@RequestMapping("/minio")
public class MinioController {

    private static final Logger log = LoggerFactory.getLogger(MinioController.class);

    @Resource
    private MinioUtil minioUtil;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            String objectName = minioUtil.uploadFile(file);
            String url = minioUtil.getPresignedUrl(objectName);
            result.put("code", 200);
            result.put("msg", "上传成功");
            Map<String, String> data = new HashMap<>();
            data.put("objectName", objectName);
            data.put("url", url);
            result.put("data", data);
        } catch (Exception e) {
            log.error("Upload failed", e);
            result.put("code", 500);
            result.put("msg", "上传失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{objectName}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String objectName) {
        try {
            InputStream inputStream = minioUtil.downloadFile(objectName);
            InputStreamResource resource = new InputStreamResource(inputStream);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + URLEncoder.encode(objectName, "UTF-8"))
                    .body(resource);
        } catch (Exception e) {
            log.error("Download failed", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取文件预览 URL
     */
    @GetMapping("/preview/{objectName}")
    public Map<String, Object> preview(@PathVariable String objectName) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = minioUtil.getPresignedUrl(objectName);
            result.put("code", 200);
            result.put("msg", "成功");
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            result.put("data", data);
        } catch (Exception e) {
            log.error("Preview failed", e);
            result.put("code", 500);
            result.put("msg", "获取预览地址失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete/{objectName}")
    public Map<String, Object> delete(@PathVariable String objectName) {
        Map<String, Object> result = new HashMap<>();
        try {
            minioUtil.removeFile(objectName);
            result.put("code", 200);
            result.put("msg", "删除成功");
        } catch (Exception e) {
            log.error("Delete failed", e);
            result.put("code", 500);
            result.put("msg", "删除失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 检查文件是否存在
     */
    @GetMapping("/exists/{objectName}")
    public Map<String, Object> exists(@PathVariable String objectName) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean exists = minioUtil.fileExists(objectName);
            result.put("code", 200);
            result.put("msg", "成功");
            result.put("data", exists);
        } catch (Exception e) {
            log.error("Check existence failed", e);
            result.put("code", 500);
            result.put("msg", "检查失败: " + e.getMessage());
        }
        return result;
    }
}
