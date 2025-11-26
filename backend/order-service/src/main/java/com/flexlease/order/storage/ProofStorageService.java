package com.flexlease.order.storage;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.order.config.ProofStorageProperties;
import jakarta.annotation.PostConstruct;
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
                "/proofs/" + generatedName,
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

    private String resolveExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx <= 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String originalName, String storedName, String fileUrl, String contentType, long size) {
    }
}
