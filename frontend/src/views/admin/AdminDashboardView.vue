<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { loadAdminDashboard, type AdminDashboardData } from './adminDashboard'

const data = ref<AdminDashboardData | null>(null)
const loading = ref(true)
const statusLabels: Record<string, string> = { PENDING_PAYMENT: '待支付', PENDING_SHIPMENT: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
const metric = (value: number | null | undefined) => value == null ? '暂不可用' : String(value)

async function load() {
  loading.value = true
  data.value = await loadAdminDashboard()
  loading.value = false
}

onMounted(load)
</script>

<template>
  <main class="workspace-page">
    <header class="workspace-page-header"><div><p class="eyebrow">管理员后台</p><h1>平台概览</h1><p>集中处理农户和商品审核，查看平台订单。</p></div><button class="secondary-button" type="button" :disabled="loading" @click="load">刷新数据</button></header>
    <section v-if="loading" class="workspace-empty">正在汇总平台数据...</section>
    <template v-else-if="data">
      <p v-if="data.errors.length" class="workspace-warning" role="alert">部分数据暂时无法加载，其余管理信息仍可正常查看。</p>
      <section class="workspace-metrics" aria-label="平台指标"><article><span>待审核商品</span><strong>{{ metric(data.stats.pendingProducts) }}</strong></article><article><span>待审核农户</span><strong>{{ metric(data.stats.pendingFarmers) }}</strong></article><article><span>已上架商品</span><strong>{{ metric(data.stats.onSaleProducts) }}</strong></article><article><span>平台订单</span><strong>{{ metric(data.stats.orders) }}</strong></article></section>
      <section class="workspace-task-strip"><div><p>商品审核</p><strong>{{ metric(data.stats.pendingProducts) }} 件待处理</strong></div><RouterLink :to="{ name: 'admin-products' }">进入审核</RouterLink><div><p>农户审核</p><strong>{{ metric(data.stats.pendingFarmers) }} 份申请待处理</strong></div><RouterLink :to="{ name: 'admin-farmers' }">进入审核</RouterLink></section>
      <section class="workspace-panel"><div class="workspace-panel-heading"><div><p class="eyebrow">平台事务</p><h2>最近订单</h2></div><RouterLink :to="{ name: 'admin-orders' }">查看全部</RouterLink></div><div v-if="data.recentOrders.length" class="workspace-order-list"><article v-for="order in data.recentOrders" :key="order.id"><div><strong>{{ order.orderNo }}</strong><span>买家 #{{ order.buyerId }} · 农户 #{{ order.farmerId }}</span></div><span class="workspace-status">{{ statusLabels[order.status] }}</span><strong>¥{{ order.totalAmount }}</strong></article></div><p v-else class="workspace-empty">平台暂时没有订单记录。</p></section>
    </template>
  </main>
</template>
