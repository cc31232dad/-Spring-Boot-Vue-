<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { OrderStatus } from '../../api/orders'
import { useOrderStore } from '../../stores/orders'

const orderStore = useOrderStore()
const isLoading = ref(true)
const loadError = ref('')
const actionError = ref('')
const shippingOrderId = ref<number | null>(null)
const actionErrorOrderId = ref<number | null>(null)

const statusLabels: Record<OrderStatus, string> = {
  PENDING_SHIPMENT: 'Ready to pack',
  SHIPPED: 'Shipped',
  COMPLETED: 'Received',
  CANCELLED: 'Cancelled'
}

onMounted(loadOrders)

async function loadOrders() {
  isLoading.value = true
  loadError.value = ''
  try {
    await orderStore.loadFarmerOrders()
  } catch {
    loadError.value = 'Farm orders could not be loaded. Please try again.'
  } finally {
    isLoading.value = false
  }
}

async function shipOrder(id: number) {
  actionError.value = ''
  actionErrorOrderId.value = null
  shippingOrderId.value = id
  try {
    await orderStore.ship(id)
  } catch {
    actionError.value = 'The shipment could not be updated. Please try again.'
    actionErrorOrderId.value = id
  } finally {
    shippingOrderId.value = null
  }
}
</script>

<template>
  <main class="orders-page">
    <header class="orders-header">
      <RouterLink class="back-link" :to="{ name: 'home' }">Back to market</RouterLink>
      <p class="eyebrow">Farm desk</p>
      <h1>Orders to harvest</h1>
      <p>Pack every order with the item details and delivery address close at hand.</p>
    </header>

    <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
    <section v-else-if="isLoading" class="orders-empty">Loading farm orders...</section>
    <section v-else-if="!orderStore.orders.length" class="orders-empty"><h2>No farm orders yet</h2><p>New customer orders will arrive here when they are placed.</p></section>
    <section v-else class="order-list" aria-label="Farm orders">
      <article v-for="order in orderStore.orders" :key="order.id" class="order-card">
        <div class="order-card-topline"><div><p class="eyebrow">Order {{ order.orderNo }}</p><p class="order-role">Buyer #{{ order.buyerId }}</p></div><span class="order-status" :class="`status-${order.status.toLowerCase()}`">{{ statusLabels[order.status] }}</span></div>
        <div class="order-card-details"><div class="order-receiver"><strong>Deliver to</strong><span>{{ order.receiverName }} · {{ order.receiverPhone }}</span><span>{{ order.receiverAddress }}</span></div><strong class="order-total">¥{{ order.totalAmount }}</strong></div>
        <ul class="order-item-list"><li v-for="item in order.items" :key="item.id" class="order-item"><img :src="item.productImageUrl" :alt="item.productName" /><div><strong>{{ item.productName }}</strong><span>{{ item.originPlace }} · {{ item.quantity }} item{{ item.quantity === 1 ? '' : 's' }}</span></div><strong>¥{{ item.subtotal }}</strong></li></ul>
        <footer class="order-card-footer"><p v-if="actionError && actionErrorOrderId === order.id" class="form-error" role="alert">{{ actionError }}</p><button v-if="order.status === 'PENDING_SHIPMENT'" type="button" :disabled="shippingOrderId === order.id" @click="shipOrder(order.id)">{{ shippingOrderId === order.id ? 'Marking shipped...' : 'Mark as shipped' }}</button></footer>
      </article>
    </section>
  </main>
</template>
