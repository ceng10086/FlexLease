package com.flexlease.product.dto;

/**
 * 商品上/下架请求（publish=true 表示上架）。
 */
public record ProductShelveRequest(boolean publish) {
}
