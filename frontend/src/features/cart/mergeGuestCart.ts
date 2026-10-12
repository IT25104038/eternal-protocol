import { customersApi } from '@/api/customers'
import { useGuestCartStore } from '@/stores/cart'

/**
 * Merges guest cart items into the authenticated user's server cart upon login 
 * and clears the local guest store
 * <p>
 * API ({@code POST /api/customers/me/cart}) automatically increments quantities 
 * for existing product, size and color combinations instead of duplicating line items
 */

export async function mergeGuestCartIntoServer(): Promise<void> {
  const { lines, clear } = useGuestCartStore.getState()
  if (lines.length === 0) return

  await Promise.all(
    lines.map((line) =>
      customersApi.cart.add({
        productId: line.productId,
        size: line.size,
        colour: line.colour,
        quantity: line.quantity,
      }),
    ),
  )

  clear()
}
