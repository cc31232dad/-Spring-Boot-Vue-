<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProduct, type ProductDetail } from '../api/products'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cartStore = useCartStore()
const product = ref<ProductDetail | null>(null)
const loadError = ref('')
const quantity = ref(1)
const addError = ref('')
const isAdding = ref(false)

onMounted(async () => {
  try {
    product.value = await getProduct(Number(route.params.id))
  } catch {
    loadError.value = '商品暂时无法加载，请稍后返回目录重试。'
  }
})

async function addToCart() {
  if (!product.value) return

  if (!auth.isLoggedIn) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }

  addError.value = ''
  isAdding.value = true

  try {
    await cartStore.addItem({ productId: product.value.id, quantity: quantity.value })
    await router.push({ name: 'cart' })
  } catch {
    addError.value = '加入购物车失败，请确认库存后重试。'
  } finally {
    isAdding.value = false
  }
}
</script>

<template>
  <main class="detail-page">
    <RouterLink class="back-link" :to="{ name: 'home' }">← 返回商品目录</RouterLink>

    <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
    <p v-else-if="!product" class="detail-loading">正在查看产地好物…</p>

    <article v-else class="product-detail">
      <div class="product-image-wrap">
        <img :src="product.imageUrl" :alt="product.name" />
      </div>
      <div class="product-copy">
        <p class="eyebrow">产地直供 · {{ product.categoryName }}</p>
        <h1>{{ product.name }}</h1>
        <p class="price">￥{{ product.price }}</p>
        <p class="product-meta">{{ product.categoryName }} · {{ product.originPlace }} · 库存 {{ product.stock }}</p>
        <div class="description-block">
          <span>商品说明</span>
          <p>{{ product.description }}</p>
        </div>
        <div class="add-to-cart">
          <label class="quantity-control">
            <span>购买数量</span>
            <input v-model.number="quantity" type="number" min="1" :max="product.stock" :disabled="product.stock < 1" />
          </label>
          <button type="button" :disabled="isAdding || product.stock < 1" @click="addToCart">
            {{ product.stock < 1 ? '暂时售罄' : isAdding ? '正在加入…' : '加入购物车' }}
          </button>
        </div>
        <p v-if="addError" class="form-error" role="alert">{{ addError }}</p>
      </div>
    </article>
  </main>
</template>

<style scoped>
.detail-page { width: min(100%, 1080px); min-height: 100vh; margin: auto; padding: clamp(24px, 6vw, 72px); }
.back-link { display: inline-flex; margin-bottom: 28px; text-underline-offset: 4px; }
.detail-loading { color: var(--muted-ink); }
.product-detail { display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(280px, .95fr); gap: clamp(28px, 5vw, 64px); padding: clamp(18px, 4vw, 38px); border-radius: 28px; background: var(--card-white); box-shadow: var(--panel-shadow); }
.product-image-wrap { min-height: 360px; overflow: hidden; border-radius: 20px 20px 20px 4px; background: var(--field-mist); }
.product-image-wrap img { display: block; width: 100%; height: 100%; object-fit: cover; }
.product-copy { align-self: center; }
.product-copy h1 { max-width: 10ch; margin-top: 8px; font-size: clamp(2.35rem, 5vw, 4.25rem); }
.product-copy .price { margin-top: 22px; font-size: clamp(1.6rem, 3vw, 2.2rem); }
.product-meta { margin: 12px 0 0; color: var(--muted-ink); line-height: 1.6; }
.description-block { margin-top: 32px; padding-top: 18px; border-top: 1px solid var(--input-border); }
.description-block span { color: var(--leaf-green); font-size: .8rem; font-weight: 800; letter-spacing: .1em; }
.description-block p { margin: 10px 0 0; color: var(--muted-ink); line-height: 1.8; white-space: pre-line; }
@media (max-width: 720px) { .product-detail { grid-template-columns: 1fr; } .product-image-wrap { min-height: 260px; } }
</style>
