import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from './http'
import { createPayment, simulatePayment } from './payment'

vi.mock('./http', () => ({ default: { post: vi.fn() } }))

describe('payment api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('creates a payment for an order', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { data: { paymentNo: 'PAY-1' } } })
    await createPayment(7)
    expect(http.post).toHaveBeenCalledWith('/orders/7/payment')
  })

  it('simulates a sandbox result', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { data: { status: 'PAID' } } })
    await simulatePayment('PAY-1', 'SUCCESS')
    expect(http.post).toHaveBeenCalledWith('/payments/sandbox/simulate', { paymentNo: 'PAY-1', result: 'SUCCESS' })
  })
})
