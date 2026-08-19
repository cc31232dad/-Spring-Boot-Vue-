import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from './http'
import { addFavorite, formatAddress, removeFavorite, selectCheckoutAddress } from './userCenter'
vi.mock('./http', () => ({ default: { post: vi.fn(), delete: vi.fn() } }))
describe('favorite api', () => {
  beforeEach(() => vi.clearAllMocks())
  it('adds a favorite and removes it by product id', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { data: { productId: 7 } } })
    await addFavorite(7)
    await removeFavorite(7)
    expect(http.post).toHaveBeenCalledWith('/favorites/7')
    expect(http.delete).toHaveBeenCalledWith('/favorites/7')
  })
  it('selects the default checkout address and formats all regions', () => {
    const addresses = [
      { id: 1, name: '甲', phone: '13800000001', province: '陕西省', city: '延安市', district: '洛川县', detail: '苹果路1号', isDefault: false },
      { id: 2, name: '乙', phone: '13800000002', province: '浙江省', city: '杭州市', district: '西湖区', detail: '茶园路2号', isDefault: true }
    ]
    expect(selectCheckoutAddress(addresses)?.id).toBe(2)
    expect(formatAddress(addresses[1])).toBe('浙江省杭州市西湖区茶园路2号')
  })})
