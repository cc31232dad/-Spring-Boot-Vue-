<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { OrderStatus } from '../../api/orders'
import { useOrderStore } from '../../stores/orders'

const orderStore = useOrderStore()
const isLoading = ref(true)
const loadError = ref('')
const actionError = ref('')
const shippingOrderIds = ref(new Set<number>())
const actionErrorOrderId = ref<number | null>(null)

const statusLabels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待支付',
  PENDING_SHIPMENT: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

onMounted(loadOrders)

async function loadOrders() {
  isLoading.value = true
  loadError.value = ''
  try {
    await orderStore.loadFarmerOrders()
  } catch {
    loadError.value = '农户订单加载失败，请稍后重试。'
  } finally {
    isLoading.value = false
  }
}

async function shipOrder(id: number) {
  if (shippingOrderIds.value.has(id)) return
  actionError.value = ''
  actionErrorOrderId.value = null
  shippingOrderIds.value = new Set(shippingOrderIds.value).add(id)
  try {
    await orderStore.ship(id)
  } catch {
    actionError.value = 'The shipment could not be updated. Please try again.'
    actionErrorOrderId.value = id
  } finally {
    const nextIds = new Set(shippingOrderIds.value)
    nextIds.delete(id)
    shippingOrderIds.value = nextIds
  }
}
</script>

<template>
  <main class="orders-page">
    <header class="orders-header">
      <p class="eyebrow">订单处理</p>
      <h1>待发货订单</h1>
      <p>核对商品与收货信息，及时完成发货。</p>
    </header>

    <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
    <section v-else-if="isLoading" class="orders-empty">正在加载农户订单...</section>
    <section v-else-if="!orderStore.orders.length" class="orders-empty"><h2>暂时没有订单</h2><p>买家下单后，相关订单会显示在这里。</p></section>
    <section v-else class="order-list" aria-label="农户订单">
      <article v-for="order in orderStore.orders" :key="order.id" class="order-card">
        <div class="order-card-topline"><div><p class="eyebrow">Order {{ order.orderNo }}</p><p class="order-role">Buyer #{{ order.buyerId }}</p></div><span class="order-status" :class="`status-${order.status.toLowerCase()}`">{{ statusLabels[order.status] }}</span></div>
        <div class="order-card-details"><div class="order-receiver"><strong>收货信息</strong><span>{{ order.receiverName }} · {{ order.receiverPhone }}</span><span>{{ order.receiverAddress }}</span></div><strong class="order-total">¥{{ order.totalAmount }}</strong></div>
        <ul class="order-item-list"><li v-for="item in order.items" :key="item.id" class="order-item"><img :src="item.productImageUrl" :alt="item.productName" /><div><strong>{{ item.productName }}</strong><span>{{ item.originPlace }} · {{ item.quantity }} 件</span></div><strong>¥{{ item.subtotal }}</strong></li></ul>
        <footer class="order-card-footer"><p v-if="actionError && actionErrorOrderId === order.id" class="form-error" role="alert">{{ actionError }}</p><button v-if="order.status === 'PENDING_SHIPMENT'" type="button" :disabled="shippingOrderIds.has(order.id)" @click="shipOrder(order.id)">{{ shippingOrderIds.has(order.id) ? '正在更新...' : '标记已发货' }}</button></footer>
      </article>
    </section>
  </main>
</template>
