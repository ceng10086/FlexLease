/**
 * 评价/口碑相关类型：
 * - 商品详情页的评价概览与列表展示复用
 */
export type ReviewSummary = {
  averageScore: number;
  totalReviews: number;
  distribution: Record<number, number>;
  responseRate?: number | null;
};

export type ReviewItem = {
  id: string;
  author: string;
  rating: number;
  content: string;
  createdAt: string;
  tags?: string[];
};
