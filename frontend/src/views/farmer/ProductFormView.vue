<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createProduct, listCategories, type Category, type ProductPayload } from '../../api/products'

const router = useRouter()
const categories = ref<Category[]>([])
const isSubmitting = ref(false)
const formError = ref('')
const form = reactive<ProductPayload>({
  categoryId: 0,
  name: '',
  description: '',
  price: 0,
  stock: 0,
  originPlace: '',
  imageUrl: ''
})

onMounted(async () => {
  try {
    categories.value = await listCategories()
  } catch {
    formError.value = '商品分类暂时无法加载，请稍后刷新页面。'
  }
})

async function submitProduct() {
  formError.value = ''
  isSubmitting.value = true

  try {
    await createProduct(form)
    await router.push({ name: 'home' })
  } catch {
    formError.value = '商品上架失败，请检查填写内容后重试。'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="farmer-form-page">
    <RouterLink class="back-link" :to="{ name: 'home' }">← 返回商品目录</RouterLink>
    <section class="form-shell" aria-labelledby="product-form-title">
      <div class="form-intro">
        <p class="eyebrow">FARMER MARKET · 新鲜上架</p>
        <h1 id="product-form-title">上架产地好物</h1>
        <p>把田间的风味和来处写清楚，让每一位顾客安心认识你的收成。</p>
      </div>

      <form class="product-form" @submit.prevent="submitProduct">
        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
        <label>
          商品分类
          <select v-model.number="form.categoryId" required>
            <option :value="0" disabled>请选择分类</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
          </select>
        </label>
        <label>
          商品名称
          <input v-model.trim="form.name" required maxlength="100" placeholder="例如：当季红富士苹果" />
        </label>
        <label>
          商品说明
          <textarea v-model.trim="form.description" required rows="5" placeholder="说说口感、采摘时间或保存建议" />
        </label>
        <div class="form-pair">
          <label>
            售价（元）
            <input v-model.number="form.price" required min="0" step="0.01" type="number" />
          </label>
          <label>
            库存
            <input v-model.number="form.stock" required min="0" step="1" type="number" />
          </label>
        </div>
        <label>
          产地
          <input v-model.trim="form.originPlace" required maxlength="100" placeholder="例如：陕西洛川" />
        </label>
        <label>
          商品图片地址
          <input v-model.trim="form.imageUrl" required type="url" placeholder="https://example.com/product.jpg" />
        </label>
        <button type="submit" :disabled="isSubmitting || !categories.length">{{ isSubmitting ? '正在上架…' : '确认上架商品' }}</button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.farmer-form-page { width: min(100%, 980px); min-height: 100vh; margin: auto; padding: clamp(24px, 6vw, 72px); }
.back-link { display: inline-flex; margin-bottom: 28px; text-underline-offset: 4px; }
.form-shell { display: grid; grid-template-columns: minmax(220px, .72fr) minmax(0, 1.28fr); gap: clamp(28px, 6vw, 72px); padding: clamp(24px, 5vw, 52px); border-radius: 28px; background: linear-gradient(135deg, #eef7e8, #fff8e6); box-shadow: var(--panel-shadow); }
.form-intro { align-self: start; padding-top: 8px; }
.form-intro h1 { color: #213226; font-size: clamp(2.25rem, 5vw, 3.7rem); }
.form-intro > p:not(.eyebrow) { color: var(--muted-ink); line-height: 1.8; }
.product-form { display: grid; gap: 18px; padding: clamp(20px, 4vw, 32px); border-radius: 20px 20px 20px 4px; background: var(--card-white); box-shadow: 0 10px 24px rgba(47, 125, 74, .08); }
.product-form label { font-size: .92rem; }
.product-form select, .product-form textarea { width: 100%; border: 1px solid var(--input-border); border-radius: 6px; padding: 12px 13px; color: var(--soil-ink); background: var(--pure-white); font: inherit; }
.product-form textarea { resize: vertical; line-height: 1.55; }
.form-pair { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
@media (max-width: 720px) { .form-shell { grid-template-columns: 1fr; } }
@media (max-width: 420px) { .form-pair { grid-template-columns: 1fr; } }
</style>
