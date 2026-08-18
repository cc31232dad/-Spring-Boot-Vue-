<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserCenterStore } from '../../stores/userCenter'
const store = useUserCenterStore()
onMounted(() => store.loadFavorites())
</script>
<template><div class="user-heading"><div><p class="eyebrow">FAVORITES</p><h1>Favorite products</h1><p class="muted-text">{{ store.favorites.length }} items</p></div></div><div v-if="!store.favorites.length" class="user-panel empty-state"><h2>No favorites yet</h2><RouterLink :to="{ name: 'home' }">Shop now</RouterLink></div><div v-else class="favorite-grid"><article v-for="item in store.favorites" :key="item.productId" class="favorite-card"><img :src="item.imageUrl" :alt="item.name"/><strong>{{ item.name }}</strong><span>{{ item.originPlace }}</span><b>¥{{ Number(item.price).toFixed(2) }}</b><button @click="store.removeFavorite(item.productId)">Remove</button></article></div></template>
