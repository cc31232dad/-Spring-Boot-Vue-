import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as productsApi from '../../api/products'
import * as ordersApi from '../../api/orders'
import { loadFarmerDashboard } from './farmerDashboard'

vi.mock('../../api/products')
vi.mock('../../api/orders')

describe('farmer dashboard', () => {
  beforeEach(() => vi.clearAllMocks())

  it('summarizes only the signed-in farmer data returned by the APIs', async () => {
    vi.mocked(productsApi.listFarmerProducts).mockResolvedValue([
      { id: 1, status: 'PENDING_REVIEW' },
      { id: 2, status: 'ON_SALE' },
      { id: 3, status: 'REJECTED' }
    ] as productsApi.ProductDetail[])
    vi.mocked(ordersApi.listFarmerOrders).mockResolvedValue([
      { id: 7, status: 'PENDING_SHIPMENT' },
      { id: 9, status: 'COMPLETED' }
    ] as ordersApi.Order[])

    const result = await loadFarmerDashboard()

    expect(result.products).toEqual({ total: 3, pending: 1, onSale: 1, rejected: 1 })
    expect(result.orders).toEqual({ total: 2, pendingShipment: 1 })
    expect(result.recentOrders.map((order) => order.id)).toEqual([9, 7])
    expect(result.errors).toEqual([])
  })

  it('retains product data when order loading fails', async () => {
    vi.mocked(productsApi.listFarmerProducts).mockResolvedValue([{ id: 1, status: 'ON_SALE' }] as productsApi.ProductDetail[])
    vi.mocked(ordersApi.listFarmerOrders).mockRejectedValue(new Error('orders failed'))

    const result = await loadFarmerDashboard()

    expect(result.products.total).toBe(1)
    expect(result.orders.total).toBeNull()
    expect(result.errors).toEqual(['orders'])
  })
})
