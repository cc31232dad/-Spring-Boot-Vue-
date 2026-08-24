<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Order, OrderStatus } from '../api/orders'
import { getOrder } from '../api/orders'
import { useCartStore } from '../stores/cart'
import { getOrderExperience, getOrderExperienceNotice } from './orderExperience'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const order = ref<Order | null>(null)
const loading = ref(true)
const error = ref('')
const actionError = ref('')
const actionNotice = ref('')
const repurchasing = ref(false)
const experience = computed(() => order.value ? getOrderExperience(order.value.status) : null)
const labels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待支付',
  PENDING_SHIPMENT: '待发货',
  SHIPPED: '配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

onMounted(async () => {
  try {
    order.value = await getOrder(Number(route.params.id))
    actionNotice.value = getOrderExperienceNotice(order.value.status, route.query.action)
  } catch {
    error.value = '订单详情加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
})

function showActionNotice(action: 'payment' | 'review') {
  if (!order.value) return
  actionNotice.value = getOrderExperienceNotice(order.value.status, action)
}

async function repurchase() {
  if (!order.value) return
  repurchasing.value = true
  actionError.value = ''
  try {
    for (const item of order.value.items) {
      await cart.addItem({ productId: item.productId, quantity: item.quantity })
    }
    await router.push({ name: 'cart' })
  } catch {
    actionError.value = '部分商品可能已下架或库存不足，请到购物车确认'
  } finally {
    repurchasing.value = false
  }
}
</script>

<template>
  <main class="orders-page">
    <header class="orders-header">
      <RouterLink class="back-link" :to="{ name: 'orders' }">返回订单列表</RouterLink>
      <p class="eyebrow">订单详情</p>
      <h1>{{ order ? `订单 ${order.orderNo}` : '订单详情' }}</h1>
      <p>查看收货信息、商品明细和配送进度。</p>
    </header>

    <section v-if="loading" class="orders-empty">正在加载订单...</section>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>
    <article v-else-if="order" class="order-card">
      <div class="order-card-topline">
        <div>
          <p class="eyebrow">订单状态</p>
          <span class="order-status" :class="`status-${order.status.toLowerCase()}`">{{ labels[order.status] }}</span>
        </div>
        <strong class="order-total">¥{{ order.totalAmount }}</strong>
      </div>

      <div class="order-card-details">
        <div class="order-receiver">
          <strong>收货信息</strong>
          <span>{{ order.receiverName }} · {{ order.receiverPhone }}</span>
          <span>{{ order.receiverAddress }}</span>
        </div>
      </div>

      <section class="logistics-placeholder">
        <p class="eyebrow">物流信息</p>
        <h2>{{ order.status === 'SHIPPED' ? '包裹运输中' : '等待发货' }}</h2>
        <p>{{ order.status === 'SHIPPED' ? '物流单号和轨迹将在接入承运商后展示。' : '农户发货后，这里会显示物流单号和配送轨迹。' }}</p>
      </section>

      <ul class="order-item-list">
        <li v-for="item in order.items" :key="item.id" class="order-item">
          <img :src="item.productImageUrl" :alt="item.productName">
          <div>
            <strong>{{ item.productName }}</strong>
            <span>{{ item.originPlace }} · {{ item.quantity }} 件</span>
          </div>
          <strong>¥{{ item.subtotal }}</strong>
        </li>
      </ul>

      <footer class="order-card-footer">
        <div class="order-action-feedback">
          <p v-if="actionError" class="form-error" role="alert">{{ actionError }}</p>
          <p v-if="actionNotice" class="order-boundary-notice" role="status">{{ actionNotice }}</p>
        </div>
        <div class="order-action-group">
          <button v-if="experience?.payment" type="button" class="primary-action" @click="showActionNotice('payment')">去支付</button>
          <button v-if="experience?.review" type="button" class="primary-action" @click="showActionNotice('review')">评价商品</button>
          <button type="button" :disabled="repurchasing || order.status === 'CANCELLED'" @click="repurchase">
            {{ repurchasing ? '正在加入购物车...' : '再次购买' }}
          </button>
        </div>
      </footer>
    </article>
  </main>
</template>
