// MILESTONE 1 SCOPE — profile + cart only.
// Deferred to Milestone 2: myOrders() (order history page) and cart.undo()
// (needs the Command-pattern backend endpoint POST /api/customers/me/cart/undo).
import { apiClient } from './client'
import type {
  AddCartItemInput,
  CartItemDto,
  CustomerProfileDto,
  UpdateProfileInput,
} from '@/types'

export const customersApi = {
  me: () => apiClient.get<CustomerProfileDto>('/api/customers/me').then((r) => r.data),

  updateMe: (input: UpdateProfileInput) =>
    apiClient.put<CustomerProfileDto>('/api/customers/me', input).then((r) => r.data),

  cart: {
    list: () => apiClient.get<CartItemDto[]>('/api/customers/me/cart').then((r) => r.data),

    /** Increments quantity if the same product+size+colour line already exists. */
    add: (input: AddCartItemInput) =>
      apiClient.post<CartItemDto>('/api/customers/me/cart', input).then((r) => r.data),

    /** Sets quantity to an exact value — use for a +/- stepper, not add(). */
    setQuantity: (itemId: number, quantity: number) =>
      apiClient.put<CartItemDto>(`/api/customers/me/cart/${itemId}`, { quantity }).then((r) => r.data),

    remove: (itemId: number) => apiClient.delete(`/api/customers/me/cart/${itemId}`),

    /** Clears every line in one action. */
    clear: () => apiClient.delete<CartItemDto[]>('/api/customers/me/cart').then((r) => r.data),
  },
}
