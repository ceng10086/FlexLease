import http from '@/api/http';
import type { ApiResponseEnvelope, CatalogProduct, PagedResult } from '@/types';
import { sampleCatalogProducts } from '@/utils/sampleData';

interface CatalogApiResponse {
  product: {
    id: string;
    vendorId?: string;
    name: string;
    categoryCode: string;
  };
  plans: {
    plan: {
      name: string;
      rentalMode: string;
      monthlyRent: number;
      depositAmount: number;
      description?: string;
    };
    skus: {
      skuCode: string;
      stockAvailable: number;
      rentAmountMonthly: number;
      depositAmount: number;
    }[];
  }[];
}

interface PagedApiResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

function mapCatalogProduct(item: CatalogApiResponse): CatalogProduct {
  const firstPlan = item.plans?.[0];
  const firstSku = firstPlan?.skus?.[0];
  return {
    id: item.product.id,
    name: item.product.name,
    category: item.product.categoryCode,
    vendorName: '合作厂商',
    cover: 'https://images.unsplash.com/photo-1520607162513-77705c0f0d4a?auto=format&fit=crop&w=800&q=80',
    pricePerMonth: Number(firstSku?.rentAmountMonthly ?? firstPlan?.plan?.monthlyRent ?? 0),
    deposit: Number(firstSku?.depositAmount ?? firstPlan?.plan?.depositAmount ?? 0),
    modes: item.plans?.map((plan) => plan.plan.rentalMode) ?? [],
    inventory: firstSku?.stockAvailable ?? 0,
    rating: 4.5,
    description: firstPlan?.plan?.description ?? '平台合作厂商提供的租赁商品，支持全流程在线管理。',
    tags: item.plans?.map((plan) => plan.plan.rentalMode) ?? [],
    planHighlights: [
      '支持在线签约与电子发票',
      '可配置履约保障与服务标准'
    ]
  };
}

export async function fetchCatalog(page = 1, size = 10): Promise<PagedResult<CatalogProduct>> {
  try {
    const { data } = await http.get<ApiResponseEnvelope<PagedApiResponse<CatalogApiResponse>>>(
      '/api/v1/catalog/products',
      {
        params: { page, size }
      }
    );
    if (!data.success) {
      throw new Error(data.message ?? '无法获取商品目录');
    }
    const content = data.data.content.map(mapCatalogProduct);
    return {
      content,
      total: data.data.totalElements,
      page: data.data.number + 1,
      size: data.data.size
    };
  } catch (error) {
    console.warn('使用示例商品目录数据', error);
    return {
      content: sampleCatalogProducts,
      total: sampleCatalogProducts.length,
      page: 1,
      size: sampleCatalogProducts.length
    };
  }
}

export async function fetchCatalogById(productId: string): Promise<CatalogProduct | undefined> {
  try {
    const { data } = await http.get<ApiResponseEnvelope<CatalogApiResponse>>(`/api/v1/catalog/products/${productId}`);
    if (!data.success) {
      throw new Error(data.message ?? '无法获取商品详情');
    }
    return mapCatalogProduct(data.data);
  } catch (error) {
    console.warn('使用示例商品详情数据', error);
    return sampleCatalogProducts.find((item) => item.id === productId) ?? sampleCatalogProducts[0];
  }
}
