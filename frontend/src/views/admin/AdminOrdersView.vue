<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { OrderStatus } from '../../api/orders'
import { useOrderStore } from '../../stores/orders'

const orderStore = useOrderStore()
const isLoading = ref(true)
const loadError = ref('')

const statusLabels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待支付',
  PENDING_SHIPMENT: 'Waiting for shipment',
  SHIPPED: 'Shipped',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled'
}

onMounted(async () => {
  try {
    await orderStore.loadAdminOrders()
  } catch {
    loadError.value = 'Orders could not be loaded. Please try again.'
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <main class="orders-page">
    <header class="orders-header">
      <RouterLink class="back-link" :to="{ name: 'home' }">Back to market</RouterLink>
      <p class="eyebrow">Market oversight</p>
      <h1>All market orders</h1>
      <p>A read-only view of every farm-to-doorstep transaction in the marketplace.</p>
    </header>

    <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
    <section v-else-if="isLoading" class="orders-empty">Loading all orders...</section>
    <section v-else-if="!orderStore.orders.length" class="orders-empty"><h2>No orders recorded</h2><p>Completed marketplace orders will be listed here.</p></section>
    <section v-else class="order-list" aria-label="All marketplace orders">
      <article v-for="order in orderStore.orders" :key="order.id" class="order-card order-card-readonly">
        <div class="order-card-topline"><div><p class="eyebrow">Order {{ order.orderNo }}</p><p class="order-role">Buyer #{{ order.buyerId }} · Farm partner #{{ order.farmerId }}</p></div><span class="order-status" :class="`status-${order.status.toLowerCase()}`">{{ statusLabels[order.status] }}</span></div>
        <div class="order-card-details"><div class="order-receiver"><strong>Receiver</strong><span>{{ order.receiverName }} · {{ order.receiverPhone }}</span><span>{{ order.receiverAddress }}</span></div><strong class="order-total">¥{{ order.totalAmount }}</strong></div>
        <ul class="order-item-list"><li v-for="item in order.items" :key="item.id" class="order-item"><img :src="item.productImageUrl" :alt="item.productName" /><div><strong>{{ item.productName }}</strong><span>{{ item.originPlace }} · {{ item.quantity }} item{{ item.quantity === 1 ? '' : 's' }}</span></div><strong>¥{{ item.subtotal }}</strong></li></ul>
      </article>
    </section>
  </main>
</template>
