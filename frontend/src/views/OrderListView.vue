<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { OrderStatus } from '../api/orders'
import { useOrderStore } from '../stores/orders'

const orderStore = useOrderStore()
const isLoading = ref(true)
const loadError = ref('')
const actionError = ref('')
const changingOrderId = ref<number | null>(null)

const statusLabels: Record<OrderStatus, string> = {
  PENDING_SHIPMENT: 'Waiting for shipment',
  SHIPPED: 'On the way',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled'
}

onMounted(loadOrders)

async function loadOrders() {
  isLoading.value = true
  loadError.value = ''
  try {
    await orderStore.loadMyOrders()
  } catch {
    loadError.value = 'Orders could not be loaded. Please try again.'
  } finally {
    isLoading.value = false
  }
}

async function updateOrder(id: number, action: 'cancel' | 'complete') {
  actionError.value = ''
  changingOrderId.value = id
  try {
    await orderStore[action](id)
  } catch {
    actionError.value = 'The order could not be updated. Please try again.'
  } finally {
    changingOrderId.value = null
  }
}
</script>

<template>
  <main class="orders-page">
    <header class="orders-header">
      <RouterLink class="back-link" :to="{ name: 'home' }">Back to market</RouterLink>
      <p class="eyebrow">My harvests</p>
      <h1>Orders from the field</h1>
      <p>Track each farm delivery and confirm when your fresh produce arrives.</p>
    </header>

    <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
    <section v-else-if="isLoading" class="orders-empty">Loading your orders...</section>
    <section v-else-if="!orderStore.orders.length" class="orders-empty">
      <h2>No orders yet</h2>
      <p>Your next seasonal find will appear here after checkout.</p>
      <RouterLink class="secondary-link" :to="{ name: 'home' }">Explore produce</RouterLink>
    </section>
    <section v-else class="order-list" aria-label="My orders">
      <article v-for="order in orderStore.orders" :key="order.id" class="order-card">
        <div class="order-card-topline">
          <div>
            <p class="eyebrow">Order {{ order.orderNo }}</p>
            <p class="order-role">Farm partner #{{ order.farmerId }}</p>
          </div>
          <span class="order-status" :class="`status-${order.status.toLowerCase()}`">{{ statusLabels[order.status] }}</span>
        </div>
        <div class="order-card-details">
          <div class="order-receiver">
            <strong>Deliver to</strong>
            <span>{{ order.receiverName }} · {{ order.receiverPhone }}</span>
            <span>{{ order.receiverAddress }}</span>
          </div>
          <strong class="order-total">¥{{ order.totalAmount }}</strong>
        </div>
        <ul class="order-item-list">
          <li v-for="item in order.items" :key="item.id" class="order-item">
            <img :src="item.productImageUrl" :alt="item.productName" />
            <div><strong>{{ item.productName }}</strong><span>{{ item.originPlace }} · {{ item.quantity }} item{{ item.quantity === 1 ? '' : 's' }}</span></div>
            <strong>¥{{ item.subtotal }}</strong>
          </li>
        </ul>
        <footer class="order-card-footer">
          <p v-if="actionError && changingOrderId === order.id" class="form-error" role="alert">{{ actionError }}</p>
          <button v-if="order.status === 'PENDING_SHIPMENT'" class="secondary-button" type="button" :disabled="changingOrderId === order.id" @click="updateOrder(order.id, 'cancel')">{{ changingOrderId === order.id ? 'Cancelling...' : 'Cancel order' }}</button>
          <button v-else-if="order.status === 'SHIPPED'" type="button" :disabled="changingOrderId === order.id" @click="updateOrder(order.id, 'complete')">{{ changingOrderId === order.id ? 'Confirming...' : 'Confirm delivery' }}</button>
        </footer>
      </article>
    </section>
  </main>
</template>
