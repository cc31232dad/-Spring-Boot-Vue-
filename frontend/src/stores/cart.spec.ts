import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as api from '../api/cart'
import { useCartStore } from './cart'

vi.mock('../api/cart', () => ({
  getCart: vi.fn(),
  addCartItem: vi.fn(),
  updateCartItem: vi.fn(),
  deleteCartItem: vi.fn(),
  clearCart: vi.fn()
}))

const cart = {
  items: [{
    id: 1,
    productId: 2,
    productName: 'Apple',
    price: 12.5,
    quantity: 2,
    stock: 10,
    originPlace: 'Shaanxi',
    imageUrl: 'https://example.com/apple.jpg',
    farmerId: 3,
    subtotal: 25
  }],
  totalAmount: 25
}

describe('cart store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads cart items and total amount', async () => {
    vi.mocked(api.getCart).mockResolvedValue(cart)
    const store = useCartStore()

    await store.loadCart()

    expect(store.items).toEqual(cart.items)
    expect(store.totalAmount).toBe(25)
  })

  it('updates a cart item quantity with the API result', async () => {
    const updatedCart = { ...cart, items: [{ ...cart.items[0], quantity: 3, subtotal: 37.5 }], totalAmount: 37.5 }
    vi.mocked(api.updateCartItem).mockResolvedValue(updatedCart)
    const store = useCartStore()

    await store.updateQuantity(1, 3)

    expect(api.updateCartItem).toHaveBeenCalledWith(1, { quantity: 3 })
    expect(store.items[0].quantity).toBe(3)
    expect(store.totalAmount).toBe(37.5)
  })

  it('does not expose checkout because checkout belongs to the order store', () => {
    const store = useCartStore()

    expect('checkout' in store).toBe(false)
  })
})
