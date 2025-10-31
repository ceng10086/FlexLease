package com.flexlease.product.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.product.dto.MediaAssetResponse;
import com.flexlease.product.dto.MediaSortUpdateRequest;
import com.flexlease.product.service.ProductMediaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/products/{productId}/media")
public class ProductMediaController {

    private final ProductMediaService productMediaService;

    public ProductMediaController(ProductMediaService productMediaService) {
        this.productMediaService = productMediaService;
    }

    @GetMapping
    public ApiResponse<List<MediaAssetResponse>> list(@PathVariable UUID vendorId,
                                                      @PathVariable UUID productId) {
        return ApiResponse.success(productMediaService.listMedia(vendorId, productId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaAssetResponse> upload(@PathVariable UUID vendorId,
                                                  @PathVariable UUID productId,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "sortOrder", required = false) Integer sortOrder) {
        return ApiResponse.success(productMediaService.upload(vendorId, productId, file, sortOrder));
    }

    @PutMapping("/{mediaId}/sort-order")
    public ApiResponse<MediaAssetResponse> updateSortOrder(@PathVariable UUID vendorId,
                                                           @PathVariable UUID productId,
                                                           @PathVariable UUID mediaId,
                                                           @Valid @RequestBody MediaSortUpdateRequest request) {
        return ApiResponse.success(productMediaService.updateSortOrder(vendorId, productId, mediaId, request.sortOrder()));
    }

    @DeleteMapping("/{mediaId}")
    public ApiResponse<Void> delete(@PathVariable UUID vendorId,
                                    @PathVariable UUID productId,
                                    @PathVariable UUID mediaId) {
        productMediaService.delete(vendorId, productId, mediaId);
        return ApiResponse.success();
    }
}
