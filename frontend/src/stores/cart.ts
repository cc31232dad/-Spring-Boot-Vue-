import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../api/cart'

export const useCartStore = defineStore('cart', () => {
  const items = ref<api.CartItem[]>([])
  const totalAmount = ref(0)

  function applyCart(cart: api.Cart) {
    items.value = cart.items
    totalAmount.value = cart.totalAmount
  }

  async function loadCart() {
    applyCart(await api.getCart())
  }

  async function addItem(payload: api.AddCartItemPayload) {
    applyCart(await api.addCartItem(payload))
  }

  async function updateQuantity(id: number, quantity: number) {
    applyCart(await api.updateCartItem(id, { quantity }))
  }

  async function removeItem(id: number) {
    await api.deleteCartItem(id)
    items.value = items.value.filter((item) => item.id !== id)
    totalAmount.value = items.value.reduce((total, item) => total + item.subtotal, 0)
  }

  async function clear() {
    await api.clearCart()
    items.value = []
    totalAmount.value = 0
  }

  return { items, totalAmount, loadCart, addItem, updateQuantity, removeItem, clear }
})
