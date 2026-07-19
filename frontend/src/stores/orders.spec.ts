import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as api from '../api/orders'
import { useOrderStore } from './orders'

vi.mock('../api/orders', () => ({
  checkout: vi.fn(),
  listMyOrders: vi.fn(),
  getOrder: vi.fn(),
  cancelOrder: vi.fn(),
  completeOrder: vi.fn(),
  listFarmerOrders: vi.fn(),
  shipOrder: vi.fn(),
  listAdminOrders: vi.fn()
}))

const order = {
  id: 1,
  orderNo: 'ORD-001',
  buyerId: 2,
  farmerId: 3,
  status: 'PENDING_SHIPMENT' as const,
  totalAmount: 25,
  receiverName: 'Alice',
  receiverPhone: '13800000000',
  receiverAddress: 'Beijing',
  items: []
}

describe('order store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('checks out with receiver information and stores returned orders', async () => {
    const payload = {
      cartItemIds: [4],
      receiverName: 'Alice',
      receiverPhone: '13800000000',
      receiverAddress: 'Beijing'
    }
    vi.mocked(api.checkout).mockResolvedValue([order])
    const store = useOrderStore()

    await store.checkoutCart(payload)

    expect(api.checkout).toHaveBeenCalledWith(payload)
    expect(store.orders).toEqual([order])
  })

  it('loads the current buyer order list', async () => {
    vi.mocked(api.listMyOrders).mockResolvedValue([order])
    const store = useOrderStore()

    await store.loadMyOrders()

    expect(store.orders).toEqual([order])
  })
})
