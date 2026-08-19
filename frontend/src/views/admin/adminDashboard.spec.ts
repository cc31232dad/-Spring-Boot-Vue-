import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as productsApi from '../../api/products'
import * as farmersApi from '../../api/farmers'
import * as ordersApi from '../../api/orders'
import { loadAdminDashboard } from './adminDashboard'

vi.mock('../../api/products')
vi.mock('../../api/farmers')
vi.mock('../../api/orders')

describe('administrator dashboard', () => {
  beforeEach(() => vi.clearAllMocks())

  it('summarizes existing administrator APIs', async () => {
    vi.mocked(productsApi.listAdminProducts)
      .mockResolvedValueOnce([{ id: 1 }] as productsApi.ProductDetail[])
      .mockResolvedValueOnce([{ id: 2 }, { id: 3 }] as productsApi.ProductDetail[])
    vi.mocked(farmersApi.listFarmerApplications).mockResolvedValue([{ id: 4 }] as never)
    vi.mocked(ordersApi.listAdminOrders).mockResolvedValue([{ id: 5 }, { id: 6 }] as ordersApi.Order[])

    const result = await loadAdminDashboard()

    expect(result.stats).toEqual({ pendingProducts: 1, onSaleProducts: 2, pendingFarmers: 1, orders: 2 })
    expect(result.recentOrders.map((order) => order.id)).toEqual([6, 5])
    expect(result.errors).toEqual([])
  })

  it('retains successful modules when pending products fail', async () => {
    vi.mocked(productsApi.listAdminProducts)
      .mockRejectedValueOnce(new Error('pending failed'))
      .mockResolvedValueOnce([])
    vi.mocked(farmersApi.listFarmerApplications).mockResolvedValue([])
    vi.mocked(ordersApi.listAdminOrders).mockResolvedValue([{ id: 8 }] as ordersApi.Order[])

    const result = await loadAdminDashboard()

    expect(result.stats.pendingProducts).toBeNull()
    expect(result.stats.orders).toBe(1)
    expect(result.errors).toEqual(['pendingProducts'])
  })
})
