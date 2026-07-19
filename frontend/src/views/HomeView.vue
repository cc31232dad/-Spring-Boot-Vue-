<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useProductStore } from '../stores/products'

const auth = useAuthStore()
const productStore = useProductStore()
const router = useRouter()
const searchText = ref('')
const loadError = ref('')

onMounted(async () => {
  await loadCatalog()

  if (auth.isLoggedIn) {
    try {
      await auth.loadCurrentUser()
    } catch {
      // The shared HTTP interceptor clears only invalid (401) sessions.
      // Keep a valid local session through transient account-loading failures.
    }
  }
})

async function loadCatalog() {
  loadError.value = ''

  try {
    await productStore.loadCatalog()
  } catch {
    loadError.value = '商品目录暂时无法加载，请稍后刷新重试。'
  }
}

async function searchProducts() {
  loadError.value = ''

  try {
    await productStore.search(searchText.value)
  } catch {
    loadError.value = '搜索暂时不可用，请稍后重试。'
  }
}

async function chooseCategory(id?: number) {
  loadError.value = ''

  try {
    await productStore.selectCategory(id)
  } catch {
    loadError.value = '分类商品暂时无法加载，请稍后重试。'
  }
}

async function clearFilters() {
  loadError.value = ''

  try {
    searchText.value = ''
    await productStore.clearFilters()
  } catch {
    loadError.value = '清除筛选条件失败，请稍后重试。'
  }
}

async function logout() {
  auth.clearSession()
  await router.replace('/login')
}
</script>

<template>
  <main class="home-page">
    <header class="home-header">
      <RouterLink class="brand-link" :to="{ name: 'home' }" aria-label="助农商城首页">
        <span class="brand-mark">助农</span>
        <span>
          <span class="eyebrow">AGRICULTURAL ASSISTANCE MARKET</span>
          <strong>助农商城</strong>
        </span>
      </RouterLink>
      <div class="account-actions">
        <template v-if="auth.isLoggedIn">
          <span class="account-name">你好，{{ auth.username || '农友' }}</span>
          <button class="secondary-button" type="button" @click="logout">退出登录</button>
        </template>
        <RouterLink v-else class="secondary-link" :to="{ name: 'login' }">登录</RouterLink>
      </div>
    </header>

    <section class="catalog-hero" aria-labelledby="catalog-title">
      <div>
        <p class="eyebrow">从田间到餐桌</p>
        <h1 id="catalog-title">把产地好物带回家</h1>
        <p class="hero-copy">挑一份当季鲜味，也为认真耕作的农户添一份踏实的收成。</p>
      </div>
      <div class="hero-badges" aria-label="商城服务特点">
        <span>产地直供</span>
        <span>新鲜到家</span>
        <span>助力农户</span>
      </div>
    </section>

    <section aria-labelledby="product-list-title">
      <div class="catalog-toolbar">
        <label class="search-field">
          <span class="sr-only">搜索农产品</span>
          <input v-model="searchText" placeholder="搜索农产品，如苹果、茶叶" @keyup.enter="searchProducts" />
        </label>
        <button type="button" class="search-button" @click="searchProducts">搜索</button>
        <div class="category-list" aria-label="商品分类">
          <button type="button" :class="{ 'category-active': productStore.categoryId === undefined }" @click="chooseCategory(undefined)">全部</button>
          <button v-for="category in productStore.categories" :key="category.id" type="button" :class="{ 'category-active': productStore.categoryId === category.id }" @click="chooseCategory(category.id)">
            {{ category.name }}
          </button>
        </div>
      </div>

      <div class="catalog-heading">
        <div>
          <p class="eyebrow">本周鲜选</p>
          <h2 id="product-list-title">来自好山好水的实在风味</h2>
        </div>
        <span class="product-count">共 {{ productStore.products.length }} 件</span>
      </div>

      <div v-if="loadError" class="catalog-error" role="alert">
        <p class="form-error">{{ loadError }}</p>
        <button type="button" @click="loadCatalog">重新加载商品</button>
      </div>
      <div v-else-if="productStore.products.length" class="product-grid">
        <RouterLink v-for="product in productStore.products" :key="product.id" class="product-card" :to="{ name: 'product-detail', params: { id: product.id } }">
          <img :src="product.imageUrl" :alt="product.name" />
          <div class="product-card-body">
            <span class="product-category">{{ product.categoryName }}</span>
            <h3>{{ product.name }}</h3>
            <p class="price">￥{{ product.price }}</p>
            <p class="product-meta">{{ product.originPlace }} · 库存 {{ product.stock }}</p>
          </div>
        </RouterLink>
      </div>
      <div v-else class="catalog-empty">
        <p>暂时没有找到合适的农产品。</p>
        <button type="button" @click="clearFilters">清除筛选条件</button>
      </div>
    </section>
  </main>
</template>
