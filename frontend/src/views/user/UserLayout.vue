<script setup lang="ts">
import { useAuthStore } from '../../stores/auth'
const auth = useAuthStore()
</script>
<template><main class="user-page"><div class="user-shell"><aside class="user-sidebar"><RouterLink class="user-back-link" :to="{ name: 'home' }">← 返回商品页</RouterLink><div class="user-profile-mini"><span class="avatar-placeholder">{{ (auth.username || '农').slice(0, 1).toUpperCase() }}</span><strong>{{ auth.username || '农友' }}</strong></div><nav><RouterLink :to="{ name: 'user' }">个人概览</RouterLink><RouterLink :to="{ name: 'user-orders' }">我的订单</RouterLink><RouterLink :to="{ name: 'user-address' }">收货地址</RouterLink><RouterLink :to="{ name: 'user-favorites' }">收藏商品</RouterLink><RouterLink :to="{ name: 'user-settings' }">账户设置</RouterLink></nav></aside><section class="user-content"><RouterView /></section></div></main></template>
