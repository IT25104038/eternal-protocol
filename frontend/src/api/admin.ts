// MILESTONE 1 SCOPE — order listing and athlete CRUD.
// Deferred to Milestone 2: the commissions calls.

import { apiClient } from './client'
import type { AdminOrderDto, AthleteDto, CreateAthleteInput, PaymentStatus, UpdateAthleteInput } from '@/types'

export const adminApi = {
  orders: {
    list: (status?: PaymentStatus) =>
      apiClient.get<AdminOrderDto[]>('/api/admin/orders', { params: status ? { status } : {} }).then((r) => r.data),
  },

  athletes: {
    list: () => apiClient.get<AthleteDto[]>('/api/admin/athletes').then((r) => r.data),

    create: (input: CreateAthleteInput) =>
      apiClient.post<AthleteDto>('/api/admin/athletes', input).then((r) => r.data),

    /** Partial update — only send the fields actually changed. */
    update: (id: number, input: UpdateAthleteInput) =>
      apiClient.put<AthleteDto>(`/api/admin/athletes/${id}`, input).then((r) => r.data),
  },

  // ----------------------------------------------------------------
  // MILESTONE 2 — commissions: { list, markPaid }
  // ----------------------------------------------------------------
}
