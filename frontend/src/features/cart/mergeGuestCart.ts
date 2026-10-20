import axios from 'axios'
import { customersApi } from '@/api/customers'
import { useGuestCartStore } from '@/stores/cart'

/**
 * Merges guest cart items into the authenticated user's server cart upon login
 * <p>
 * API ({@code POST /api/customers/me/cart}) automatically increments quantities
 * for existing product, size and color combinations instead of duplicating line items
 * <p>
 * Each line is synced on its own, so one bad line can't strand the rest:
 * <ul>
 *   <li>synced lines are removed from the guest cart</li>
 *   <li>lines the server refuses with 409 (a COMING_SOON product that can't be
 *   bought yet) can never sync, so they are dropped rather than retried forever</li>
 *   <li>lines that fail for any other reason (e.g. a network error) stay in the
 *   guest cart so nothing the customer chose is silently lost</li>
 * </ul>
 * If any line didn't make it across, this throws afterwards so the caller can
 * warn the customer to check their cart
 */

export async function mergeGuestCartIntoServer(): Promise<void> {
  const { lines, remove } = useGuestCartStore.getState()
  if (lines.length === 0) return

  const results = await Promise.allSettled(
    lines.map((line) =>
      customersApi.cart.add({
        productId: line.productId,
        size: line.size,
        colour: line.colour,
        quantity: line.quantity,
      }),
    ),
  )

  let skipped = false
  results.forEach((result, i) => {
    if (result.status === 'fulfilled') {
      remove(lines[i].lineId)
      return
    }
    skipped = true
    const notAvailable = axios.isAxiosError(result.reason) && result.reason.response?.status === 409
    if (notAvailable) remove(lines[i].lineId)
  })

  if (skipped) throw new Error('Some guest cart items could not be added to your cart')
}
