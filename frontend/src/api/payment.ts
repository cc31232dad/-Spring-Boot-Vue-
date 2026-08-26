import http from './http'

export interface Payment {
  paymentNo: string
  orderNo: string
  amount: number
  channel: 'SANDBOX'
  status: 'PENDING' | 'PAID' | 'FAILED' | 'CLOSED'
  expiresAt: string
  paidAt?: string
}

export async function createPayment(orderId: number): Promise<Payment> {
  const { data } = await http.post(`/orders/${orderId}/payment`)
  return data.data as Payment
}

export async function simulatePayment(paymentNo: string, result: 'SUCCESS' | 'FAILURE'): Promise<Payment> {
  const { data } = await http.post('/payments/sandbox/simulate', { paymentNo, result })
  return data.data as Payment
}
