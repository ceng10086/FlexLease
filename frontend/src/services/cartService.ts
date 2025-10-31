import http from './http';

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export type CartItem = {
  id: string;
  userId: string;
  vendorId: string;
  productId: string;
  skuId: string;
  planId?: string | null;
  productName: string;
  skuCode?: string | null;
  planSnapshot?: string | null;
  quantity: number;
  unitRentAmount: number;
  unitDepositAmount: number;
  buyoutPrice?: number | null;
  createdAt: string;
  updatedAt: string;
};

export type AddCartItemPayload = {
  userId: string;
  vendorId: string;
  productId: string;
  skuId: string;
  planId?: string | null;
  productName: string;
  skuCode?: string | null;
  planSnapshot?: string | null;
  quantity: number;
  unitRentAmount: number;
  unitDepositAmount: number;
  buyoutPrice?: number | null;
};

export type UpdateCartItemPayload = {
  userId: string;
  quantity: number;
};

export const fetchCartItems = async (userId: string): Promise<CartItem[]> => {
  const response = await http.get<ApiResponse<CartItem[]>>('/cart', { params: { userId } });
  return response.data.data;
};

export const addCartItem = async (payload: AddCartItemPayload): Promise<CartItem> => {
  const response = await http.post<ApiResponse<CartItem>>('/cart/items', payload);
  return response.data.data;
};

export const updateCartItem = async (itemId: string, payload: UpdateCartItemPayload): Promise<CartItem> => {
  const response = await http.put<ApiResponse<CartItem>>(`/cart/items/${itemId}`, payload);
  return response.data.data;
};

export const removeCartItem = async (itemId: string, userId: string): Promise<void> => {
  await http.delete<ApiResponse<void>>(`/cart/items/${itemId}`, { params: { userId } });
};

export const clearCart = async (userId: string): Promise<void> => {
  await http.delete<ApiResponse<void>>('/cart', { params: { userId } });
};
