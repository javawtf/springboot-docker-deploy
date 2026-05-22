package com.example.demo;

import com.example.demo.util.MinioUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * MinIO 上传测试 — 从本地文件系统拉取图片并上传到 MinIO
 */
@SpringBootTest
class MinioUploadTest {

    private static final Logger log = LoggerFactory.getLogger(MinioUploadTest.class);

    @Resource
    private MinioUtil minioUtil;

    /**
     * 测试：从本地路径读取一张图片并上传到 MinIO
     * 请修改 IMAGE_PATH 为你本地实际的图片路径
     */
    @Test
    void testUploadLocalImage() throws Exception {
        // ========== 修改这里为你本地的图片路径 ==========
        String imagePath = "C:\\Users\\Admin\\Desktop\\图片\\91de7e94881b20370755473765844402d38ca83f.jpg";
        // =============================================

        File file = new File(imagePath);
        if (!file.exists()) {
            log.error("图片不存在: {}", imagePath);
            log.info("请将图片放到: {} 后重新运行测试", imagePath);
            return;
        }

        String fileName = file.getName();
        String contentType = Files.probeContentType(Paths.get(imagePath));
        if (contentType == null) {
            contentType = "image/jpeg";
        }

        log.info("开始上传 — 文件: {}, 类型: {}, 大小: {} bytes", fileName, contentType, file.length());

        // 上传到 MinIO
        long start = System.currentTimeMillis();
        InputStream inputStream = new FileInputStream(file);
        minioUtil.uploadFile(inputStream, fileName, contentType, file.length());
        inputStream.close();
        long cost = System.currentTimeMillis() - start;

        log.info("上传成功 — 对象名: {}, 耗时: {}ms", fileName, cost);

        // 获取预签名 URL
        String presignedUrl = minioUtil.getPresignedUrl(fileName);
        log.info("预签名访问地址: {}", presignedUrl);

        // 验证文件是否存在
        boolean exists = minioUtil.fileExists(fileName);
        log.info("文件存在性验证: {}", exists ? "通过" : "失败");
    }
}
