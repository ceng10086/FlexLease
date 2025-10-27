export type UserRole = 'CUSTOMER' | 'VENDOR' | 'ADMIN';

export interface ApiResponseEnvelope<T> {
  success: boolean;
  data: T;
  code?: string;
  message?: string;
}

export interface UserProfile {
  id: string;
  username: string;
  roles: UserRole[];
  lastLoginAt?: string | null;
}

export interface DashboardMetric {
  title: string;
  value: string | number;
  trend?: number;
  trendLabel?: string;
}

export interface CatalogProduct {
  id: string;
  name: string;
  category: string;
  vendorName: string;
  cover: string;
  pricePerMonth: number;
  deposit: number;
  modes: string[];
  inventory: number;
  rating: number;
  description: string;
  tags: string[];
  planHighlights: string[];
}

export interface PagedResult<T> {
  content: T[];
  total: number;
  page: number;
  size: number;
}

export interface OrderTimelineItem {
  label: string;
  time: string;
  status: 'completed' | 'pending' | 'warning';
}

export interface RentalOrderSummary {
  id: string;
  productName: string;
  vendorName: string;
  planName: string;
  mode: string;
  status: string;
  startDate: string;
  endDate: string;
  nextAction?: string;
  amountDue: number;
  timeline: OrderTimelineItem[];
}

export interface PaymentRecord {
  id: string;
  orderId: string;
  amount: number;
  method: string;
  status: string;
  createdAt: string;
}

export interface VendorProductSummary {
  id: string;
  name: string;
  status: string;
  rentalModes: string[];
  totalInventory: number;
  leased: number;
  pendingOrders: number;
  lastUpdated: string;
}

export interface VendorOrderSummary {
  id: string;
  customerName: string;
  productName: string;
  status: string;
  shipBy: string;
  nextStep: string;
  mode: string;
  value: number;
}

export interface VendorMetric {
  label: string;
  value: string;
  trend: number;
  unit?: string;
}

export interface AdminProductReview {
  id: string;
  vendorName: string;
  productName: string;
  submittedAt: string;
  rentalModes: string[];
  status: string;
  riskScore: number;
}

export interface SettlementRecord {
  id: string;
  vendorName: string;
  period: string;
  orderCount: number;
  grossAmount: number;
  refundAmount: number;
  netAmount: number;
  status: string;
}

export interface VendorApplicationSummary {
  id: string;
  companyName: string;
  contact: string;
  status: string;
  submittedAt: string;
  remark?: string;
}
