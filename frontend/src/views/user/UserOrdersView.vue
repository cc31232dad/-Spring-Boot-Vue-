<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as api from '../../api/userCenter'
import type { Order } from '../../api/orders'
const route = useRoute(); const router = useRouter(); const orders = ref<Order[]>([]); const loading = ref(false)
const tabs = [['', 'All'], ['wait', 'To Pay'], ['ship', 'To Receive'], ['done', 'Completed']]
const statusLabel = (s: string) => ({ PENDING_PAYMENT: 'To Pay', PENDING_SHIPMENT: 'To Ship', SHIPPED: 'To Receive', COMPLETED: 'Completed', CANCELLED: 'Cancelled' } as Record<string, string>)[s] || s
async function load() { loading.value = true; try { orders.value = await api.listOrders(String(route.query.status || '')) } finally { loading.value = false } }
function select(status: string) { router.push({ name: 'user-orders', query: status ? { status } : undefined }) }
async function cancel(id: number) { if (confirm('Cancel this order?')) { await api.cancelOrder(id); await load() } }
async function confirmReceive(id: number) { if (confirm('Confirm receipt?')) { await api.confirmOrder(id); await load() } }
onMounted(load); watch(() => route.query.status, load)
</script>
<template><div class="user-heading"><div><p class="eyebrow">ORDERS</p><h1>My Orders</h1></div></div><div class="order-tabs"><button v-for="tab in tabs" :key="tab[0]" :class="{active:String(route.query.status || '') === tab[0]}" @click="select(tab[0])">{{ tab[1] }}</button></div><div v-if="loading" class="user-panel">Loading...</div><div v-else-if="!orders.length" class="user-panel empty-state"><h2>No orders yet</h2><RouterLink :to="{ name: 'home' }">Shop now</RouterLink></div><div v-else class="user-panel order-list"><article v-for="order in orders" :key="order.id" class="user-order"><img :src="order.items[0]?.productImageUrl" alt="product"/><div class="order-main"><strong>{{ order.items[0]?.productName || 'Farm product order' }}</strong><small>{{ order.orderNo }}</small><small>{{ order.receiverAddress }}</small></div><b>¥{{ order.totalAmount.toFixed(2) }}</b><span class="status-chip">{{ statusLabel(order.status) }}</span><div class="order-actions"><button v-if="order.status === 'PENDING_PAYMENT'" @click="cancel(order.id)">Cancel</button><button v-if="order.status === 'SHIPPED'" @click="confirmReceive(order.id)">Confirm</button><button v-if="order.status === 'PENDING_PAYMENT'" class="primary-action">Pay</button></div></article></div></template>
