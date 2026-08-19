<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { OrderStatus } from '../../api/orders'
import { useOrderStore } from '../../stores/orders'

const orderStore = useOrderStore()
const isLoading = ref(true)
const loadError = ref('')

const statusLabels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待支付',
  PENDING_SHIPMENT: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

onMounted(async () => {
  try {
    await orderStore.loadAdminOrders()
  } catch {
    loadError.value = '平台订单加载失败，请稍后重试。'
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <main class="orders-page">
    <header class="orders-header">
      <p class="eyebrow">订单管理</p>
      <h1>平台订单</h1>
      <p>查看平台内全部农产品订单及履约状态。</p>
    </header>

    <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
    <section v-else-if="isLoading" class="orders-empty">正在加载平台订单...</section>
    <section v-else-if="!orderStore.orders.length" class="orders-empty"><h2>暂无订单记录</h2><p>平台产生订单后会显示在这里。</p></section>
    <section v-else class="order-list" aria-label="平台订单">
      <article v-for="order in orderStore.orders" :key="order.id" class="order-card order-card-readonly">
        <div class="order-card-topline"><div><p class="eyebrow">订单 {{ order.orderNo }}</p><p class="order-role">买家 #{{ order.buyerId }} · 农户 #{{ order.farmerId }}</p></div><span class="order-status" :class="`status-${order.status.toLowerCase()}`">{{ statusLabels[order.status] }}</span></div>
        <div class="order-card-details"><div class="order-receiver"><strong>收货信息</strong><span>{{ order.receiverName }} · {{ order.receiverPhone }}</span><span>{{ order.receiverAddress }}</span></div><strong class="order-total">¥{{ order.totalAmount }}</strong></div>
        <ul class="order-item-list"><li v-for="item in order.items" :key="item.id" class="order-item"><img :src="item.productImageUrl" :alt="item.productName" /><div><strong>{{ item.productName }}</strong><span>{{ item.originPlace }} · {{ item.quantity }} 件</span></div><strong>¥{{ item.subtotal }}</strong></li></ul>
      </article>
    </section>
  </main>
</template>
