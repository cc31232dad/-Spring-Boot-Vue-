import http from './http'

export interface SeckillActivity {
  id: number
  productId: number
  farmerId: number
  status: 'DRAFT' | 'PUBLISHED' | 'ENDED'
  seckillPrice: number
  totalStock: number
  limitPerUser: number
  startAt: string
  endAt: string
}

export interface SeckillRushPayload {
  receiverName: string
  receiverPhone: string
  receiverAddress: string
}

export async function listActivities(): Promise<SeckillActivity[]> {
  const { data } = await http.get('/seckill')
  return data.data as SeckillActivity[]
}

export async function rush(id: number, payload: SeckillRushPayload) {
  const { data } = await http.post(`/seckill/${id}/rush`, payload)
  return data.data
}

export async function cancel(id: number) {
  await http.patch(`/seckill/${id}/cancel`)
}
