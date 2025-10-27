import http from './http';

export type PagedResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type CatalogRentalPlan = {
  id: string;
  planType: string;
  termMonths: number;
  depositAmount: number;
  rentAmountMonthly: number;
  buyoutPrice?: number | null;
  allowExtend: boolean;
  extensionUnit?: string | null;
  extensionPrice?: number | null;
  skus: CatalogSku[];
};

export type CatalogSku = {
  id: string;
  skuCode: string;
  attributes?: Record<string, unknown> | null;
  stockAvailable: number;
  stockTotal: number;
};

export type CatalogProductSummary = {
  id: string;
  name: string;
  categoryCode: string;
  coverImageUrl?: string | null;
  description?: string | null;
  status: string;
  rentalPlans: Array<{
    id: string;
    planType: string;
    termMonths: number;
    rentAmountMonthly: number;
    depositAmount: number;
    buyoutPrice?: number | null;
  }>;
};

export type CatalogProductDetail = {
  id: string;
  vendorId: string;
  name: string;
  categoryCode: string;
  coverImageUrl?: string | null;
  description?: string | null;
  status: string;
  rentalPlans: CatalogRentalPlan[];
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export const listCatalogProducts = async (params: {
  keyword?: string;
  categoryCode?: string;
  page?: number;
  size?: number;
} = {}): Promise<PagedResponse<CatalogProductSummary>> => {
  const response = await http.get<ApiResponse<PagedResponse<ApiCatalogProduct>>>(
    '/catalog/products',
    { params }
  );
  const data = response.data.data;
  return {
    ...data,
    content: data.content.map(mapToSummary)
  };
};

export const fetchCatalogProduct = async (productId: string): Promise<CatalogProductDetail> => {
  const response = await http.get<ApiResponse<ApiCatalogProduct>>(`/catalog/products/${productId}`);
  return mapToDetail(response.data.data);
};

type ApiCatalogProduct = {
  id: string;
  vendorId: string;
  name: string;
  categoryCode: string;
  description?: string | null;
  coverImageUrl?: string | null;
  status: string;
  rentalPlans: Array<ApiCatalogRentalPlan>;
};

type ApiCatalogRentalPlan = {
  id: string;
  planType: string;
  termMonths: number;
  depositAmount: number;
  rentAmountMonthly: number;
  buyoutPrice?: number | null;
  allowExtend: boolean;
  extensionUnit?: string | null;
  extensionPrice?: number | null;
  skus: Array<ApiCatalogSku>;
};

type ApiCatalogSku = {
  id: string;
  skuCode: string;
  attributes?: Record<string, unknown> | null;
  stockTotal: number;
  stockAvailable: number;
};

const mapToSummary = (product: ApiCatalogProduct): CatalogProductSummary => ({
  id: product.id,
  name: product.name,
  categoryCode: product.categoryCode,
  coverImageUrl: product.coverImageUrl,
  description: product.description,
  status: product.status,
  rentalPlans: product.rentalPlans.map((plan) => ({
    id: plan.id,
    planType: plan.planType,
    termMonths: plan.termMonths,
    rentAmountMonthly: plan.rentAmountMonthly,
    depositAmount: plan.depositAmount,
    buyoutPrice: plan.buyoutPrice ?? null
  }))
});

const mapToDetail = (product: ApiCatalogProduct): CatalogProductDetail => ({
  id: product.id,
  vendorId: product.vendorId,
  name: product.name,
  categoryCode: product.categoryCode,
  coverImageUrl: product.coverImageUrl,
  description: product.description,
  status: product.status,
  rentalPlans: product.rentalPlans.map((plan) => ({
    id: plan.id,
    planType: plan.planType,
    termMonths: plan.termMonths,
    depositAmount: plan.depositAmount,
    rentAmountMonthly: plan.rentAmountMonthly,
    buyoutPrice: plan.buyoutPrice ?? null,
    allowExtend: plan.allowExtend,
    extensionUnit: plan.extensionUnit,
    extensionPrice: plan.extensionPrice,
    skus: plan.skus.map((sku) => ({
      id: sku.id,
      skuCode: sku.skuCode,
      attributes: sku.attributes ?? null,
      stockAvailable: sku.stockAvailable,
      stockTotal: sku.stockTotal
    }))
  }))
});
