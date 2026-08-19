<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { approveProduct, listAdminProducts, rejectProduct, type ProductDetail, type ProductStatus } from '../../api/products'

const products = ref<ProductDetail[]>([])
const status = ref<ProductStatus>('PENDING_REVIEW')
const loading = ref(true)
const error = ref('')
const busyId = ref<number | null>(null)
const rejectReasons = ref<Record<number, string>>({})
const labels: Record<ProductStatus, string> = { PENDING_REVIEW: '待审核', ON_SALE: '已上架', OFF_SALE: '已下架', REJECTED: '已拒绝' }

async function load() { loading.value = true; error.value = ''; try { products.value = await listAdminProducts(status.value) } catch { error.value = '商品审核列表加载失败，请稍后重试' } finally { loading.value = false } }
async function approve(id: number) { busyId.value = id; try { await approveProduct(id); await load() } catch { error.value = '通过审核失败，请稍后重试' } finally { busyId.value = null } }
async function reject(id: number) { if (!window.confirm('确认拒绝这个商品吗？')) return; const reason = (rejectReasons.value[id] ?? '').trim(); if (!reason) { error.value = '请先填写拒绝原因'; return }; busyId.value = id; try { await rejectProduct(id, reason); await load() } catch { error.value = '拒绝商品失败，请稍后重试' } finally { busyId.value = null } }
onMounted(load)
</script>
<template>
  <main class="orders-page product-management-page">
    <header class="orders-header"><RouterLink class="back-link" :to="{ name: 'home' }">返回商城</RouterLink><p class="eyebrow">平台管理</p><h1>商品审核</h1><p>审核农户提交的商品信息，维护商城商品质量。</p></header>
    <div class="management-toolbar"><label>审核状态<select v-model="status" @change="load"><option value="PENDING_REVIEW">待审核</option><option value="ON_SALE">已上架</option><option value="REJECTED">已拒绝</option><option value="OFF_SALE">已下架</option></select></label><button class="secondary-button" type="button" @click="load">刷新列表</button></div>
    <p v-if="error" class="form-error" role="alert">{{ error }}</p><section v-if="loading" class="orders-empty">正在加载商品...</section><section v-else-if="!products.length" class="orders-empty"><h2>暂无商品</h2><p>当前筛选条件下没有商品。</p></section>
    <section v-else class="order-list" aria-label="商品审核列表"><article v-for="product in products" :key="product.id" class="order-card product-review-card"><div class="product-review-main"><img :src="product.imageUrl" :alt="product.name"><div><p class="eyebrow">{{ product.categoryName }} · 农户 #{{ product.farmerId }}</p><h2>{{ product.name }}</h2><p>{{ product.originPlace }} · 库存 {{ product.stock }} · ¥{{ product.price }}</p><p v-if="product.reviewReason" class="review-reason">拒绝原因：{{ product.reviewReason }}</p></div><span class="order-status" :class="`status-${product.status.toLowerCase()}`">{{ labels[product.status] }}</span></div><div v-if="product.status === 'PENDING_REVIEW'" class="review-actions"><button type="button" :disabled="busyId === product.id" @click="approve(product.id)">通过审核</button><div><input v-model="rejectReasons[product.id]" placeholder="填写拒绝原因"><button class="secondary-button" type="button" :disabled="busyId === product.id" @click="reject(product.id)">拒绝</button></div></div></article></section>
  </main>
</template>
