import http from '@/api/http';
import type { AdminProductReview, ApiResponseEnvelope, PagedResult } from '@/types';
import { sampleAdminReviews } from '@/utils/sampleData';

interface AdminProductApiModel {
  id: string;
  vendorName: string;
  productName: string;
  rentalModes: string[];
  submittedAt: string;
  status: string;
  riskScore: number;
}

export async function fetchProductReviews(status?: string): Promise<AdminProductReview[]> {
  try {
    const { data } = await http.get<ApiResponseEnvelope<PagedResult<AdminProductApiModel>>>('/api/v1/admin/products', {
      params: { status }
    });
    if (!data.success) {
      throw new Error(data.message ?? '无法获取商品审核数据');
    }
    return data.data.content.map((item) => ({
      id: item.id,
      vendorName: item.vendorName,
      productName: item.productName,
      rentalModes: item.rentalModes,
      submittedAt: item.submittedAt,
      status: item.status,
      riskScore: item.riskScore
    }));
  } catch (error) {
    console.warn('使用示例商品审核数据', error);
    return sampleAdminReviews;
  }
}
