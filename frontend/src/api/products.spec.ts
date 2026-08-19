import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from './http'
import { approveProduct, listAdminProducts, listFarmerProducts, rejectProduct, updateProduct } from './products'

vi.mock('./http', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn() } }))

describe('product review api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('lists products awaiting admin review with status filter', async () => {
    vi.mocked(http.get).mockResolvedValue({ data: { data: [{ id: 1, status: 'PENDING_REVIEW' }] } })
    await expect(listAdminProducts('PENDING_REVIEW')).resolves.toHaveLength(1)
    expect(http.get).toHaveBeenCalledWith('/admin/products/review', { params: { status: 'PENDING_REVIEW' } })
  })

  it('sends approve and reject actions', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { data: { id: 1, status: 'ON_SALE' } } })
    await approveProduct(1)
    await rejectProduct(1, '图片不清晰')
    expect(http.post).toHaveBeenNthCalledWith(1, '/admin/products/1/approve')
    expect(http.post).toHaveBeenNthCalledWith(2, '/admin/products/1/reject', { reason: '图片不清晰' })
  })

  it('updates a farmer product before resubmission', async () => {
    vi.mocked(http.put).mockResolvedValue({ data: { data: { id: 2, status: 'PENDING_REVIEW' } } })
    await updateProduct(2, { categoryId: 1, name: '苹果', description: '更新', price: 12, stock: 3, originPlace: '陕西', imageUrl: '/apple.jpg' })
    expect(http.put).toHaveBeenCalledWith('/farmer/products/2', expect.objectContaining({ name: '苹果' }))
  })

  it('lists the current farmer products', async () => {
    vi.mocked(http.get).mockResolvedValue({ data: { data: [] } })
    await expect(listFarmerProducts()).resolves.toEqual([])
    expect(http.get).toHaveBeenCalledWith('/farmer/products')
  })
})
