package com.flexlease.order.storage;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.order.config.ProofStorageProperties;
import jakarta.annotation.PostConstruct;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * 取证文件的本地存储服务。
 * <p>
 * 上传文件落盘到配置的根目录；下载时按文件名读取；图片类文件会尽力添加水印（失败不影响主流程）。
 */
@Component
public class ProofStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(ProofStorageService.class);

    private final Path rootLocation;

    public ProofStorageService(ProofStorageProperties properties) {
        this.rootLocation = Path.of(properties.getRoot()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建取证存储目录: " + rootLocation, ex);
        }
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "上传文件不能为空");
        }
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "proof" : file.getOriginalFilename());
        String extension = resolveExtension(originalFilename);
        String generatedName = UUID.randomUUID().toString().replace("-", "");
        if (!extension.isEmpty()) {
            generatedName = generatedName + "." + extension;
        }
        Path target = rootLocation.resolve(generatedName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            LOG.error("Failed to store proof file {}", generatedName, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }
        return new StoredFile(
                originalFilename,
                generatedName,
            "/api/v1/proofs/" + generatedName,
                file.getContentType(),
                file.getSize()
        );
    }

    public Resource loadAsResource(String storedName) {
        if (!StringUtils.hasText(storedName)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件不存在");
        }
        try {
            Path target = rootLocation.resolve(storedName).normalize();
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件不存在或不可读");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件地址无效");
        }
    }

    public void delete(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return;
        }
        Path path = rootLocation.resolve(fileName).normalize();
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            LOG.warn("Failed to delete proof file {}: {}", fileName, ex.getMessage());
        }
    }

    public void applyWatermark(String storedName, String contentType, String watermarkText) {
        if (!StringUtils.hasText(storedName) || !StringUtils.hasText(watermarkText)) {
            return;
        }
        String extension = resolveExtension(storedName);
        boolean extensionLooksSupported = "png".equals(extension) || "jpg".equals(extension) || "jpeg".equals(extension);
        boolean contentTypeLooksImage = StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
        if (contentTypeLooksImage) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("image/png") && !normalized.startsWith("image/jpeg") && !normalized.startsWith("image/jpg")) {
                return;
            }
        } else if (!extensionLooksSupported) {
            return;
        }
        Path path = rootLocation.resolve(storedName).normalize();
        try {
            if (!Files.exists(path)) {
                return;
            }
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                return;
            }
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            graphics.setColor(Color.WHITE);
            int fontSize = Math.max(20, image.getWidth() / 25);
            graphics.setFont(resolveWatermarkFont(fontSize));
            FontMetrics metrics = graphics.getFontMetrics();
            int x = Math.max(10, image.getWidth() - metrics.stringWidth(watermarkText) - 20);
            int y = Math.max(metrics.getHeight(), image.getHeight() - metrics.getDescent() - 20);
            graphics.drawString(watermarkText, x, y);
            graphics.dispose();
            String format = "png".equals(extension) ? "png" : "jpg";
            ImageIO.write(image, format, path.toFile());
        } catch (Throwable ex) {
            // 水印是“尽力而为”：字体/图片处理不可用时也不应导致上传失败
            LOG.warn("Failed to apply watermark to proof {}: {}", storedName, ex.toString());
        }
    }

    private String resolveExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx <= 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private Font resolveWatermarkFont(int size) {
        // 在精简容器里优先选择能渲染中文的字体，避免水印出现方块。
        String[] preferredFamilies = {
                "Noto Sans CJK SC",
                "Noto Sans CJK",
                "WenQuanYi Zen Hei",
                "Microsoft YaHei"
        };
        try {
            String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for (String preferred : preferredFamilies) {
                for (String family : available) {
                    if (preferred.equalsIgnoreCase(family)) {
                        return new Font(family, Font.BOLD, size);
                    }
                }
            }
        } catch (Exception ignored) {
            // 兜底：字体列表获取失败时使用默认字体
        }
        return new Font(Font.SANS_SERIF, Font.BOLD, size);
    }

    public record StoredFile(String originalName, String storedName, String fileUrl, String contentType, long size) {
    }
}
