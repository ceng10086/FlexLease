package com.flexlease.product.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.product.domain.MediaAsset;
import com.flexlease.product.domain.Product;
import com.flexlease.product.dto.MediaAssetResponse;
import com.flexlease.product.repository.MediaAssetRepository;
import com.flexlease.product.repository.ProductRepository;
import com.flexlease.product.storage.FileStorageService;
import com.flexlease.product.storage.FileStorageService.StoredFile;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ProductMediaService {

    /**
     * 商品媒体资源管理服务。
     * <p>
     * 文件先写入本地存储，再落库保存元信息与排序；删除时会同时删除数据库记录与本地文件。
     */
    private final ProductRepository productRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final FileStorageService fileStorageService;
    private final ProductAssembler productAssembler;

    public ProductMediaService(ProductRepository productRepository,
                               MediaAssetRepository mediaAssetRepository,
                               FileStorageService fileStorageService,
                               ProductAssembler productAssembler) {
        this.productRepository = productRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.fileStorageService = fileStorageService;
        this.productAssembler = productAssembler;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<MediaAssetResponse> listMedia(UUID vendorId, UUID productId) {
        Product product = getProduct(vendorId, productId);
        return product.getMediaAssets().stream()
                .sorted(Comparator.comparing(MediaAsset::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MediaAsset::getCreatedAt))
                .map(productAssembler::toMediaAssetResponse)
                .toList();
    }

    public MediaAssetResponse upload(UUID vendorId, UUID productId, MultipartFile file, Integer sortOrder) {
        Product product = getProduct(vendorId, productId);
        StoredFile stored = fileStorageService.store(file);
        Integer resolvedSort = sortOrder != null ? sortOrder : nextSortOrder(product.getId());
        MediaAsset asset = MediaAsset.create(product, stored.fileName(), stored.url(), stored.contentType(), stored.size(), resolvedSort);
        product.addMediaAsset(asset);
        MediaAsset saved = mediaAssetRepository.save(asset);
        return productAssembler.toMediaAssetResponse(saved);
    }

    public MediaAssetResponse updateSortOrder(UUID vendorId, UUID productId, UUID mediaId, Integer sortOrder) {
        if (sortOrder == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "sortOrder 不能为空");
        }
        MediaAsset asset = loadAsset(vendorId, productId, mediaId);
        asset.updateSortOrder(sortOrder);
        return productAssembler.toMediaAssetResponse(asset);
    }

    public void delete(UUID vendorId, UUID productId, UUID mediaId) {
        MediaAsset asset = loadAsset(vendorId, productId, mediaId);
        String fileName = asset.getFileName();
        asset.getProduct().removeMediaAsset(asset);
        mediaAssetRepository.delete(asset);
        fileStorageService.delete(fileName);
    }

    private Product getProduct(UUID vendorId, UUID productId) {
        return productRepository.findWithPlansByIdAndVendorId(productId, vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在"));
    }

    private MediaAsset loadAsset(UUID vendorId, UUID productId, UUID mediaId) {
        MediaAsset asset = mediaAssetRepository.findByIdAndProductId(mediaId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "媒体资源不存在"));
        if (!asset.getProduct().getVendorId().equals(vendorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该媒体资源");
        }
        return asset;
    }

    private int nextSortOrder(UUID productId) {
        return mediaAssetRepository.findByProductIdOrderBySortOrderAscCreatedAtAsc(productId)
                .stream()
                .map(MediaAsset::getSortOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .map(order -> order + 1)
                .orElse(0);
    }
}
