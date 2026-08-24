<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserCenterStore } from '../../stores/userCenter'
const store = useUserCenterStore()
onMounted(() => store.loadFavorites())
</script>
<template><div class="user-heading"><div><p class="eyebrow">收藏管理</p><h1>收藏商品</h1><p class="muted-text">共 {{ store.favorites.length }} 件商品</p></div></div><div v-if="!store.favorites.length" class="user-panel empty-state"><h2>暂无收藏商品</h2><RouterLink :to="{ name: 'home' }">去逛逛</RouterLink></div><div v-else class="favorite-grid"><article v-for="item in store.favorites" :key="item.productId" class="favorite-card"><img :src="item.imageUrl" :alt="item.name"/><strong>{{ item.name }}</strong><span>{{ item.originPlace }}</span><b>¥{{ Number(item.price).toFixed(2) }}</b><button @click="store.removeFavorite(item.productId)">取消收藏</button></article></div></template>
