package com.flexlease.product.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.product.dto.FileUploadResponse;
import com.flexlease.product.storage.FileStorageService;
import com.flexlease.product.storage.FileStorageService.StoredFile;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ProductAssetService {

    /**
     * 资产/文件相关的轻量封装服务。
     * <p>
     * 当前主要用于封面图上传（返回可访问 URL）与临时文件删除。
     */
    private final FileStorageService fileStorageService;

    public ProductAssetService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public FileUploadResponse uploadCoverImage(MultipartFile file) {
        if (file == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请提供需要上传的封面图片");
        }
        StoredFile stored = fileStorageService.store(file);
        return new FileUploadResponse(stored.fileName(), stored.url(), stored.contentType(), stored.size());
    }

    public void deleteTemporaryFile(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "文件标识不能为空");
        }
        fileStorageService.delete(fileName);
    }
}
