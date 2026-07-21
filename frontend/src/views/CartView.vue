<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { useOrderStore } from '../stores/orders'

const router = useRouter()
const cartStore = useCartStore()
const orderStore = useOrderStore()
const loadError = ref('')
const actionError = ref('')
const isLoading = ref(true)
const isCheckingOut = ref(false)
const changingItemId = ref<number | null>(null)
const receiver = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

const itemCount = computed(() => cartStore.items.reduce((count, item) => count + item.quantity, 0))

onMounted(loadCart)

async function loadCart() {
  loadError.value = ''
  isLoading.value = true
  try {
    await cartStore.loadCart()
  } catch {
    loadError.value = '购物车暂时无法加载，请稍后重试。'
  } finally {
    isLoading.value = false
  }
}

async function changeQuantity(id: number, quantity: number, event: Event) {
  const item = cartStore.items.find((cartItem) => cartItem.id === id)
  const input = event.target as HTMLInputElement
  if (!item) return

  if (!Number.isInteger(quantity) || quantity < 1) {
    input.value = String(item.quantity)
    return
  }

  if (quantity > item.stock) {
    input.value = String(item.quantity)
    actionError.value = '购买数量不能超过当前库存。'
    return
  }

  actionError.value = ''
  changingItemId.value = id
  try {
    await cartStore.updateQuantity(id, quantity)
  } catch {
    actionError.value = '数量更新失败，请按库存重新选择。'
  } finally {
    changingItemId.value = null
  }
}

async function removeItem(id: number) {
  actionError.value = ''
  changingItemId.value = id
  try {
    await cartStore.removeItem(id)
  } catch {
    actionError.value = '移除商品失败，请稍后重试。'
  } finally {
    changingItemId.value = null
  }
}

async function checkout() {
  if (!cartStore.items.length) return
  actionError.value = ''
  isCheckingOut.value = true
  try {
    await orderStore.checkoutCart({
      cartItemIds: cartStore.items.map((item) => item.id),
      ...receiver
    })
  } catch {
    actionError.value = '结算未完成，请检查收货信息或稍后重试。'
    isCheckingOut.value = false
    return
  }

  try {
    await cartStore.loadCart()
    if (router.hasRoute('orders')) {
      await router.push({ name: 'orders' })
    }
  } catch {
    cartStore.items = []
    cartStore.totalAmount = 0
    actionError.value = '订单已提交，但页面刷新失败。请稍后到我的订单中查看。'
  } finally {
    isCheckingOut.value = false
  }
}
</script>

<template>
  <main class="cart-shell">
    <header class="cart-header">
      <RouterLink class="back-link" :to="{ name: 'home' }">← 继续挑选</RouterLink>
      <p class="eyebrow">我的收获篮</p>
      <h1>把田间好味带回家</h1>
      <p>确认商品与收货信息后，我们会把新鲜心意交给产地。</p>
    </header>

    <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
    <div v-else-if="isLoading" class="cart-empty">正在整理你的收获篮…</div>
    <section v-else-if="!cartStore.items.length" class="cart-empty">
      <h2>收获篮还是空的</h2>
      <p>去看看当季新鲜农产，把喜欢的商品加入购物车。</p>
      <RouterLink class="secondary-link" :to="{ name: 'home' }">返回商品目录</RouterLink>
    </section>

    <section v-else class="cart-layout" aria-label="购物车结算">
      <div class="cart-list">
        <article v-for="item in cartStore.items" :key="item.id" class="cart-item">
          <img :src="item.imageUrl" :alt="item.productName" />
          <div class="cart-item-copy">
            <p class="eyebrow">{{ item.originPlace }}</p>
            <h2>{{ item.productName }}</h2>
            <p class="price">¥{{ item.price }}</p>
            <p class="cart-stock">库存 {{ item.stock }} 件</p>
          </div>
          <div class="cart-item-actions">
            <label class="quantity-control">
              <span class="sr-only">{{ item.productName }} 数量</span>
              <input :value="item.quantity" type="number" min="1" :max="item.stock" :disabled="changingItemId === item.id" @change="changeQuantity(item.id, Number(($event.target as HTMLInputElement).value), $event)" />
            </label>
            <strong>¥{{ item.subtotal }}</strong>
            <button class="text-button" type="button" :disabled="changingItemId === item.id" @click="removeItem(item.id)">移除</button>
          </div>
        </article>

        <form id="receiver-form" class="receiver-form" @submit.prevent="checkout">
          <div>
            <p class="eyebrow">送达信息</p>
            <h2>收货人信息</h2>
          </div>
          <label>收货人姓名<input v-model.trim="receiver.receiverName" required autocomplete="name" /></label>
          <label>手机号码<input v-model.trim="receiver.receiverPhone" required type="tel" autocomplete="tel" /></label>
          <label>详细地址<textarea v-model.trim="receiver.receiverAddress" required rows="3" autocomplete="street-address" /></label>
        </form>
      </div>

      <aside class="checkout-panel">
        <p class="eyebrow">Harvest basket</p>
        <h2>本次收获</h2>
        <p class="checkout-count">{{ itemCount }} 件农产 · {{ cartStore.items.length }} 个品类</p>
        <div class="checkout-total"><span>合计</span><strong>¥{{ cartStore.totalAmount }}</strong></div>
        <p class="checkout-note">订单将按农户分别安排发货。</p>
        <p v-if="actionError" class="form-error" role="alert">{{ actionError }}</p>
        <button form="receiver-form" type="submit" :disabled="isCheckingOut">{{ isCheckingOut ? '正在提交订单…' : '确认并提交订单' }}</button>
      </aside>
    </section>
  </main>
</template>
