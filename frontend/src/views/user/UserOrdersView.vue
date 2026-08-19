<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as api from '../../api/userCenter'
import type { Order } from '../../api/orders'
const route = useRoute(); const router = useRouter(); const orders = ref<Order[]>([]); const loading = ref(false)
const tabs = [['', '全部'], ['wait', '待付款'], ['ship', '待收货'], ['done', '已完成']]
const statusLabel = (s: string) => ({ PENDING_PAYMENT: '待付款', PENDING_SHIPMENT: '待发货', SHIPPED: '待收货', COMPLETED: '已完成', CANCELLED: '已取消' } as Record<string, string>)[s] || s
async function load() { loading.value = true; try { orders.value = await api.listOrders(String(route.query.status || '')) } finally { loading.value = false } }
function select(status: string) { router.push({ name: 'user-orders', query: status ? { status } : undefined }) }
async function cancel(id: number) { if (confirm('确定取消此订单吗？')) { await api.cancelOrder(id); await load() } }
async function confirmReceive(id: number) { if (confirm('确认已经收到商品吗？')) { await api.confirmOrder(id); await load() } }
onMounted(load); watch(() => route.query.status, load)
</script>
<template><div class="user-heading"><div><p class="eyebrow">用户中心</p><h1>我的订单</h1></div></div><div class="order-tabs"><button v-for="tab in tabs" :key="tab[0]" :class="{active:String(route.query.status || '') === tab[0]}" @click="select(tab[0])">{{ tab[1] }}</button></div><div v-if="loading" class="user-panel">加载中...</div><div v-else-if="!orders.length" class="user-panel empty-state"><h2>暂无相关订单</h2><RouterLink :to="{ name: 'home' }">去逛逛</RouterLink></div><div v-else class="user-panel order-list"><article v-for="order in orders" :key="order.id" class="user-order"><img :src="order.items[0]?.productImageUrl" alt="商品"/><div class="order-main"><strong>{{ order.items[0]?.productName || '农产品订单' }}</strong><small>订单号：{{ order.orderNo }}</small><small>{{ order.receiverAddress }}</small></div><b>¥{{ order.totalAmount.toFixed(2) }}</b><span class="status-chip">{{ statusLabel(order.status) }}</span><div class="order-actions"><button v-if="order.status === 'PENDING_PAYMENT'" @click="cancel(order.id)">取消订单</button><button v-if="order.status === 'SHIPPED'" @click="confirmReceive(order.id)">确认收货</button><button v-if="order.status === 'PENDING_PAYMENT'" class="primary-action">去支付</button><RouterLink class="order-detail-link" :to="{ name: 'order-detail', params: { id: order.id } }">详情</RouterLink></div></article></div></template>
