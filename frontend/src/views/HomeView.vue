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
    try { await auth.loadCurrentUser() } catch { /* Keep the local session on transient failures. */ }
  }
})

async function loadCatalog() {
  loadError.value = ''
  try { await productStore.loadCatalog() } catch { loadError.value = '商品目录暂时无法加载，请稍后刷新重试。' }
}

async function searchProducts() {
  loadError.value = ''
  try { await productStore.search(searchText.value) } catch { loadError.value = '搜索暂时不可用，请稍后重试。' }
}

async function chooseCategory(id?: number) {
  loadError.value = ''
  try { await productStore.selectCategory(id) } catch { loadError.value = '分类商品暂时无法加载，请稍后重试。' }
}

async function clearFilters() {
  loadError.value = ''
  try { searchText.value = ''; await productStore.clearFilters() } catch { loadError.value = '清除筛选条件失败，请稍后重试。' }
}

function soldCount(productId: number) { return 120 + ((productId * 37) % 9800) }

async function logout() { auth.clearSession(); await router.replace('/login') }
</script>

<template>
  <main class="home-page">
    <div class="market-topbar"><span>助农商城 · 源头直采，安心到家</span><span>满 99 元包邮　|　新客首单立减</span></div>
    <header class="home-header">
      <RouterLink class="brand-link" :to="{ name: 'home' }" aria-label="助农商城首页"><span class="brand-mark">助农</span><span><span class="eyebrow">AGRICULTURAL ASSISTANCE MARKET</span><strong>助农商城</strong></span></RouterLink>
      <div class="account-actions"><RouterLink class="secondary-link" :to="{ name: 'seckill' }">秒杀专区</RouterLink><RouterLink class="secondary-link" :to="{ name: 'cart' }">购物车</RouterLink><template v-if="auth.isLoggedIn"><details class="user-dropdown"><summary><span class="avatar-placeholder small">{{ (auth.username || '农').slice(0, 1).toUpperCase() }}</span>{{ auth.username || '农友' }}<i>3</i></summary><div class="dropdown-menu"><RouterLink :to="{name:'user'}">个人中心</RouterLink><RouterLink :to="{name:'user-orders'}">我的订单</RouterLink><RouterLink :to="{name:'user-address'}">收货地址</RouterLink><RouterLink :to="{name:'user-favorites'}">收藏商品</RouterLink><RouterLink :to="{name:'user-settings'}">账户设置</RouterLink><button type="button" @click="logout">退出登录</button></div></details></template><RouterLink v-else class="secondary-link" :to="{ name: 'login' }">登录</RouterLink></div>
    </header>

    <nav class="market-nav" aria-label="商城导航"><strong>全部商品分类</strong><a href="#product-list-title">精选农品</a><RouterLink :to="{ name: 'seckill' }">限时秒杀</RouterLink><span class="market-nav-note">每一份订单，都在支持真实乡村产业</span></nav>

    <section class="catalog-hero" aria-labelledby="catalog-title"><div><p class="eyebrow">从田间到餐桌</p><h1 id="catalog-title">把产地好物带回家</h1><p class="hero-copy">挑一份当季鲜味，也为认真耕作的农户添一份踏实的收成。</p></div><div class="hero-badges" aria-label="商城服务特点"><span>产地直供</span><span>新鲜到家</span><span>助力农户</span></div></section>
    <section class="assistance-strip" aria-label="助农服务"><span class="strip-kicker">助农精选</span><span class="strip-item">严选当季鲜果</span><span class="strip-item">农户直发无中间商</span><span class="strip-item">每笔订单可追溯</span><span class="strip-item">破损包赔，放心尝鲜</span></section>

    <section aria-labelledby="product-list-title"><div class="catalog-toolbar"><label class="search-field"><span class="sr-only">搜索农产品</span><input v-model="searchText" placeholder="搜索农产品，如苹果、茶叶" @keyup.enter="searchProducts" /></label><button type="button" class="search-button" @click="searchProducts">搜索</button><div class="category-list" aria-label="商品分类"><button type="button" :class="{ 'category-active': productStore.categoryId === undefined }" @click="chooseCategory(undefined)">全部</button><button v-for="category in productStore.categories" :key="category.id" type="button" :class="{ 'category-active': productStore.categoryId === category.id }" @click="chooseCategory(category.id)">{{ category.name }}</button></div></div>
      <div class="catalog-heading"><div><p class="eyebrow">本周鲜选</p><h2 id="product-list-title">来自好山好水的实在风味</h2><p v-if="productStore.isUsingMockData" class="demo-notice">当前展示的是演示农产品数据，后端商品上架后会自动切换为真实目录。</p></div><span class="product-count">共 {{ productStore.products.length }} 件</span></div>
      <div v-if="loadError" class="catalog-error" role="alert"><p class="form-error">{{ loadError }}</p><button type="button" @click="loadCatalog">重新加载商品</button></div><div v-else-if="productStore.products.length" class="product-grid"><RouterLink v-for="product in productStore.products" :key="product.id" class="product-card" :to="{ name: 'product-detail', params: { id: product.id } }"><img :src="product.imageUrl" :alt="product.name" loading="lazy" /><div class="product-card-body"><span class="product-category">{{ product.categoryName }}</span><h3>{{ product.name }}</h3><div class="product-tags"><span>产地直供</span><span>助农优选</span></div><div class="product-buy-row"><p class="price">¥{{ product.price.toFixed(2) }}</p><span class="sold-count">已售 {{ soldCount(product.id) }}+</span></div><p class="product-meta">{{ product.originPlace }}　|　库存 {{ product.stock }}</p><span class="card-action">查看详情 →</span></div></RouterLink></div><div v-else class="catalog-empty"><p>暂时没有找到合适的农产品。</p><button type="button" @click="clearFilters">清除筛选条件</button></div>
    </section>
  </main>
</template>
