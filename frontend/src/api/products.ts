import { apiClient } from './client'
import type { ProductCategory, ProductDto, ProductInput } from '@/types'

export interface ProductQuery {
  colour?: string
  size?: string
  styleCode?: string
  category?: ProductCategory
  subCategory?: string
}

export const productsApi = {
  list: (query: ProductQuery = {}) =>
    apiClient.get<ProductDto[]>('/api/products', { params: query }).then((r) => r.data),

  /** All colour/size variants of one style — what the PDP selector should call. */
  byStyle: (styleCode: string) =>
    apiClient.get<ProductDto[]>('/api/products', { params: { styleCode } }).then((r) => r.data),

  get: (id: number) =>
    apiClient.get<ProductDto>(`/api/products/${id}`).then((r) => r.data),

  /** Distinct subcategories currently in use within a category — powers the navbar's dropdowns. */
  subCategories: (category: ProductCategory) =>
    apiClient.get<string[]>('/api/products/subcategories', { params: { category } }).then((r) => r.data),

  create: (input: ProductInput) =>
    apiClient.post<ProductDto>('/api/products', input).then((r) => r.data),

  update: (id: number, input: ProductInput) =>
    apiClient.put<ProductDto>(`/api/products/${id}`, input).then((r) => r.data),

  remove: (id: number) => apiClient.delete(`/api/products/${id}`)
}
