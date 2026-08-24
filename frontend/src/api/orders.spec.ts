import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from './http'
import { getOrder } from './orders'
vi.mock('./http', () => ({ default: { get: vi.fn() } }))
describe('order api', () => {
  beforeEach(() => vi.clearAllMocks())
  it('loads an order detail by id', async () => {
    vi.mocked(http.get).mockResolvedValue({ data: { data: { id: 9, orderNo: 'A9' } } })
    await expect(getOrder(9)).resolves.toMatchObject({ id: 9 })
    expect(http.get).toHaveBeenCalledWith('/orders/9')
  })
})
