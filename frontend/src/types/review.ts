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
