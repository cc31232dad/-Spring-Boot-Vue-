<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { loadFarmerDashboard, type FarmerDashboardData } from './farmerDashboard'

const data = ref<FarmerDashboardData | null>(null)
const loading = ref(true)
const statusLabels: Record<string, string> = { PENDING_PAYMENT: '待支付', PENDING_SHIPMENT: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
const metric = (value: number | null | undefined) => value == null ? '暂不可用' : String(value)

async function load() {
  loading.value = true
  data.value = await loadFarmerDashboard()
  loading.value = false
}

onMounted(load)
</script>

<template>
  <main class="workspace-page">
    <header class="workspace-page-header"><div><p class="eyebrow">农户工作台</p><h1>经营概览</h1><p>集中查看商品审核状态和待处理订单。</p></div><button class="secondary-button" type="button" :disabled="loading" @click="load">刷新数据</button></header>
    <section v-if="loading" class="workspace-empty">正在汇总经营数据...</section>
    <template v-else-if="data">
      <p v-if="data.errors.length" class="workspace-warning" role="alert">部分数据暂时无法加载，其余经营信息仍可正常查看。</p>
      <section class="workspace-metrics" aria-label="经营指标"><article><span>全部商品</span><strong>{{ metric(data.products.total) }}</strong></article><article><span>待审核</span><strong>{{ metric(data.products.pending) }}</strong></article><article><span>已上架</span><strong>{{ metric(data.products.onSale) }}</strong></article><article><span>待发货订单</span><strong>{{ metric(data.orders.pendingShipment) }}</strong></article></section>
      <section class="workspace-task-strip"><div><p>待处理商品</p><strong>{{ metric(data.products.pending) }} 件等待平台审核</strong></div><RouterLink :to="{ name: 'farmer-products' }">查看商品</RouterLink><div><p>待处理订单</p><strong>{{ metric(data.orders.pendingShipment) }} 笔等待发货</strong></div><RouterLink :to="{ name: 'farmer-orders' }">处理订单</RouterLink></section>
      <section class="workspace-panel"><div class="workspace-panel-heading"><div><p class="eyebrow">最近事务</p><h2>最近订单</h2></div><RouterLink :to="{ name: 'farmer-orders' }">查看全部</RouterLink></div><div v-if="data.recentOrders.length" class="workspace-order-list"><article v-for="order in data.recentOrders" :key="order.id"><div><strong>{{ order.orderNo }}</strong><span>买家 #{{ order.buyerId }}</span></div><span class="workspace-status">{{ statusLabels[order.status] }}</span><strong>¥{{ order.totalAmount }}</strong></article></div><p v-else class="workspace-empty">暂时没有订单，后续订单会显示在这里。</p></section>
    </template>
  </main>
</template>
