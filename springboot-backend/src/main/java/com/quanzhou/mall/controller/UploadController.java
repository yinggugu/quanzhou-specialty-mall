package com.quanzhou.mall.controller;

import com.quanzhou.mall.config.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final String UPLOAD_DIR = "static/uploads/";
    private static final int MAX_WIDTH = 1200;

    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ApiResponse.error("文件为空");
        String type = file.getContentType();
        if (type == null || (!type.equals("image/jpeg") && !type.equals("image/png")))
            return ApiResponse.error("仅支持jpg/png格式");
        if (file.getSize() > 10 * 1024 * 1024) return ApiResponse.error("图片不能超过10MB");

        try {
            // 确保目录存在
            Path dir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            // 生成唯一文件名
            String name = UUID.randomUUID().toString() + ".jpg";
            Path dest = dir.resolve(name);

            // 压缩
            BufferedImage img = ImageIO.read(file.getInputStream());
            if (img == null) return ApiResponse.error("无法解析图片");
            int w = img.getWidth(), h = img.getHeight();
            if (w > MAX_WIDTH) { h = h * MAX_WIDTH / w; w = MAX_WIDTH; }
            BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, w, h, null);
            g.dispose();
            ImageIO.write(resized, "jpg", dest.toFile());

            String url = "/" + UPLOAD_DIR + name;
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            return ApiResponse.ok("上传成功", result);
        } catch (IOException e) {
            log.warn("图片上传失败", e);
            return ApiResponse.error("上传失败");
        }
    }
}
