package com.flexlease.product.storage;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.product.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageService.class);

    private final Path rootLocation;

    public FileStorageService(StorageProperties properties) {
        this.rootLocation = Path.of(properties.getRoot()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建文件存储目录: " + rootLocation, ex);
        }
    }

    public StoredFile store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "上传文件不能为空");
        }
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        String generatedName = UUID.randomUUID().toString().replace("-", "");
        if (!extension.isEmpty()) {
            generatedName = generatedName + "." + extension;
        }
        Path destination = rootLocation.resolve(generatedName);
        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            LOG.error("Failed to store file {}", generatedName, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }
        String url = "/media/" + generatedName;
        return new StoredFile(generatedName, url, file.getContentType(), file.getSize());
    }

    public void delete(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return;
        }
        Path path = rootLocation.resolve(fileName).normalize();
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            LOG.warn("Failed to delete file {}: {}", fileName, ex.getMessage());
        }
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx <= 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String fileName, String url, String contentType, long size) {
    }
}
