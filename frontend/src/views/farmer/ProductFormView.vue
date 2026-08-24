<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createProduct, listCategories, listFarmerProducts, updateProduct, uploadProductImage, type Category, type ProductPayload } from '../../api/products'
import { validateProductImage } from './productImage'

const router = useRouter()
const route = useRoute()
const editId = Number(route.params.id) || 0
const categories = ref<Category[]>([])
const isSubmitting = ref(false)
const isUploading = ref(false)
const dragActive = ref(false)
const formError = ref('')
const imageError = ref('')
const previewUrl = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
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
    if (editId) {
      const product = (await listFarmerProducts()).find((item) => item.id === editId)
      if (!product) throw new Error('商品不存在')
      Object.assign(form, product)
      previewUrl.value = product.imageUrl
    }
  } catch {
    formError.value = '商品分类暂时无法加载，请稍后刷新页面。'
  }
})

function chooseImage() {
  fileInput.value?.click()
}

async function selectImage(file?: File) {
  if (!file) return
  imageError.value = ''
  const validationError = validateProductImage(file)
  if (validationError) {
    imageError.value = validationError
    return
  }
  if (previewUrl.value.startsWith('blob:')) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(file)
  isUploading.value = true
  try {
    form.imageUrl = await uploadProductImage(file)
  } catch {
    form.imageUrl = ''
    imageError.value = '图片上传失败，请重试。'
  } finally {
    isUploading.value = false
  }
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  void selectImage(input.files?.[0])
  input.value = ''
}

function onDrop(event: DragEvent) {
  dragActive.value = false
  void selectImage(event.dataTransfer?.files[0])
}

function removeImage() {
  if (previewUrl.value.startsWith('blob:')) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  form.imageUrl = ''
  imageError.value = ''
}

onBeforeUnmount(() => {
  if (previewUrl.value.startsWith('blob:')) URL.revokeObjectURL(previewUrl.value)
})

async function submitProduct() {
  formError.value = ''
  isSubmitting.value = true

  try {
    if (!form.imageUrl || isUploading.value) {
      formError.value = '请先选择并上传商品图片。'
      return
    }
    if (editId) await updateProduct(editId, form)
    else await createProduct(form)
    await router.push({ name: 'farmer-products' })
  } catch {
    formError.value = '商品保存失败，请检查填写内容后重试。'
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
        <p class="eyebrow">FARMER MARKET · 商品管理</p>
        <h1 id="product-form-title">{{ editId ? '修改商品' : '上架产地好物' }}</h1>
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
        <div class="image-upload-field">
          <span class="field-label">商品图片</span>
          <input ref="fileInput" class="visually-hidden" type="file" accept="image/jpeg,image/png,image/webp" @change="onFileChange" />
          <button v-if="!previewUrl" class="image-drop-zone" :class="{ 'is-drag-active': dragActive }" type="button"
            @click="chooseImage" @dragenter.prevent="dragActive = true" @dragover.prevent="dragActive = true"
            @dragleave.prevent="dragActive = false" @drop.prevent="onDrop">
            <strong>拖入商品图片，或点击选择</strong>
            <small>支持 JPG、PNG、WEBP，单张不超过 5MB；手机端可从相册选择</small>
          </button>
          <div v-else class="image-preview-panel">
            <img :src="previewUrl" alt="商品图片预览" />
            <div>
              <p>{{ isUploading ? '正在上传图片…' : '图片已准备好' }}</p>
              <button type="button" class="secondary-button" :disabled="isUploading" @click="chooseImage">更换图片</button>
              <button type="button" class="text-button" :disabled="isUploading" @click="removeImage">删除</button>
            </div>
          </div>
          <p v-if="imageError" class="form-error" role="alert">{{ imageError }}</p>
        </div>
        <button type="submit" :disabled="isSubmitting || isUploading || !categories.length || !form.imageUrl">{{ isSubmitting ? '正在保存…' : (editId ? '保存并重新提交审核' : '确认提交审核') }}</button>
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
.product-form input[type="number"] { width: 100%; border: 1px solid var(--input-border); border-radius: 6px; padding: 12px 13px; color: var(--soil-ink); background: var(--pure-white); font: inherit; }
.product-form textarea { resize: vertical; line-height: 1.55; }
.form-pair { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.image-upload-field { display: grid; gap: 8px; }
.field-label { font-size: .92rem; }
.visually-hidden { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.image-drop-zone { display: grid; gap: 8px; min-height: 150px; place-content: center; border: 1px dashed #8eb494; border-radius: 10px; padding: 24px; color: var(--soil-ink); background: #f7fbf4; text-align: center; }
.image-drop-zone:hover, .image-drop-zone.is-drag-active { border-color: var(--leaf-green); background: #edf7e9; }
.image-drop-zone small { color: var(--muted-ink); font-weight: 400; }
.image-preview-panel { display: flex; align-items: center; gap: 16px; border: 1px solid var(--input-border); border-radius: 10px; padding: 12px; }
.image-preview-panel img { width: 124px; height: 96px; border-radius: 6px; object-fit: cover; background: #f3f5ef; }
.image-preview-panel p { margin: 0 0 10px; color: var(--muted-ink); }
.image-preview-panel .secondary-button, .image-preview-panel .text-button { margin-right: 10px; }
@media (max-width: 720px) { .form-shell { grid-template-columns: 1fr; } }
@media (max-width: 420px) { .form-pair { grid-template-columns: 1fr; } }
</style>
