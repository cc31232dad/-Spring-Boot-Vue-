<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as api from '../api/seckill'
import { useAuthStore } from '../stores/auth'
import { useSeckillStore } from '../stores/seckill'

const store = useSeckillStore()
const auth = useAuthStore()
const router = useRouter()
const busyId = ref<number | null>(null)
const message = ref('')
const receiverName = ref('')
const receiverPhone = ref('')
const receiverAddress = ref('')

const now = ref(Date.now())
const activeActivities = computed(() => store.activities.filter((activity) => new Date(activity.endAt).getTime() > now.value))

function activityLabel(activity: api.SeckillActivity) {
  const start = new Date(activity.startAt).getTime()
  if (now.value < start) return '即将开始'
  return '立即抢购'
}

async function rush(activity: api.SeckillActivity) {
  if (!auth.isLoggedIn) {
    await router.push({ name: 'login', query: { redirect: '/seckill' } })
    return
  }
  busyId.value = activity.id
  message.value = ''
  try {
    await store.rush(activity.id, { receiverName: receiverName.value, receiverPhone: receiverPhone.value, receiverAddress: receiverAddress.value })
    message.value = '抢购成功，订单已生成，请尽快完成支付。'
  } catch (cause) {
    message.value = cause instanceof Error ? cause.message : '抢购失败，请稍后重试。'
  } finally {
    busyId.value = null
  }
}

onMounted(async () => {
  await store.loadActivities()
  window.setInterval(() => { now.value = Date.now() }, 1000)
})
</script>

<template>
  <main class="orders-page seckill-page">
    <header class="orders-header">
      <RouterLink class="back-link" :to="{ name: 'home' }">返回商城</RouterLink>
      <p class="eyebrow">限时助农</p>
      <h1>今日秒杀</h1>
      <p>把平台补贴和农户好收成，留给正在认真生活的人。</p>
    </header>

    <section class="receiver-form" aria-labelledby="receiver-title">
      <h2 id="receiver-title">收货信息</h2>
      <label>姓名<input v-model="receiverName" required /></label>
      <label>电话<input v-model="receiverPhone" required /></label>
      <label>地址<textarea v-model="receiverAddress" rows="2" required /></label>
    </section>
    <p v-if="message" class="form-error" role="status">{{ message }}</p>
    <p v-if="store.error" class="form-error" role="alert">{{ store.error }}</p>
    <section v-if="activeActivities.length" class="seckill-grid" aria-label="秒杀活动列表">
      <article v-for="activity in activeActivities" :key="activity.id" class="seckill-card">
        <p class="eyebrow">活动 #{{ activity.id }}</p>
        <h2>产地直发限时价</h2>
        <p class="price">¥{{ activity.seckillPrice }}</p>
        <p class="product-meta">库存 {{ activity.totalStock }} · 每人限购 {{ activity.limitPerUser }} 件</p>
        <button type="button" :disabled="busyId === activity.id || activityLabel(activity) !== '立即抢购'" @click="rush(activity)">{{ busyId === activity.id ? '提交中…' : activityLabel(activity) }}</button>
      </article>
    </section>
    <div v-else class="orders-empty"><h2>暂时没有进行中的秒杀</h2><p>新的农产活动准备好后，会第一时间出现在这里。</p></div>
  </main>
</template>
