import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../api/orders'

export const useOrderStore = defineStore('orders', () => {
  const orders = ref<api.Order[]>([])
  const currentOrder = ref<api.Order | null>(null)

  function replaceOrder(order: api.Order) {
    const index = orders.value.findIndex((existingOrder) => existingOrder.id === order.id)
    if (index >= 0) {
      orders.value.splice(index, 1, order)
    }
    currentOrder.value = order
  }

  async function checkoutCart(payload: api.CheckoutPayload) {
    orders.value = await api.checkout(payload)
  }

  async function loadMyOrders() {
    orders.value = await api.listMyOrders()
  }

  async function cancel(id: number) {
    replaceOrder(await api.cancelOrder(id))
  }

  async function complete(id: number) {
    replaceOrder(await api.completeOrder(id))
  }

  async function loadFarmerOrders() {
    orders.value = await api.listFarmerOrders()
  }

  async function ship(id: number) {
    replaceOrder(await api.shipOrder(id))
  }

  async function loadAdminOrders() {
    orders.value = await api.listAdminOrders()
  }

  return { orders, currentOrder, checkoutCart, loadMyOrders, cancel, complete, loadFarmerOrders, ship, loadAdminOrders }
})
