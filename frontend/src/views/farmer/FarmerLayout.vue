<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ClipboardList, LayoutDashboard, LogOut, Menu, Package, ShoppingBag, Sprout, X } from '@lucide/vue'
import { useAuthStore } from '../../stores/auth'
const auth = useAuthStore(); const route = useRoute(); const router = useRouter(); const menuOpen = ref(false)
const titles: Record<string, string> = { 'farmer-dashboard': '经营概览', 'farmer-products': '商品管理', 'farmer-product-new': '发布商品', 'farmer-product-edit': '修改商品', 'farmer-orders': '订单处理' }
const title = computed(() => titles[String(route.name)] ?? '农户工作台'); const displayName = computed(() => auth.username || '农户账号')
watch(() => route.fullPath, () => { menuOpen.value = false })
async function logout() { auth.clearSession(); await router.push({ name: 'login' }) }
</script>
<template>
  <div class="farmer-shell workspace-shell">
    <button v-if="menuOpen" class="workspace-scrim" type="button" aria-label="关闭导航" @click="menuOpen = false"></button>
    <aside class="workspace-sidebar" :class="{ open: menuOpen }"><div class="workspace-brand"><span class="workspace-brand-mark"><Sprout :size="22" /></span><div><strong>助农商城</strong><span>农户工作台</span></div><button class="workspace-close" type="button" aria-label="关闭导航" @click="menuOpen = false"><X :size="20" /></button></div>
      <nav aria-label="农户工作台导航"><RouterLink :to="{ name: 'farmer-dashboard' }"><LayoutDashboard :size="19" /><span>工作台</span></RouterLink><RouterLink :to="{ name: 'farmer-products' }"><Package :size="19" /><span>商品管理</span></RouterLink><RouterLink :to="{ name: 'farmer-orders' }"><ClipboardList :size="19" /><span>订单处理</span></RouterLink></nav>
      <div class="workspace-sidebar-footer"><RouterLink :to="{ name: 'home' }"><ShoppingBag :size="19" /><span>查看商城</span></RouterLink><button type="button" @click="logout"><LogOut :size="19" /><span>退出登录</span></button></div>
    </aside><section class="workspace-main"><header class="workspace-topbar"><button class="workspace-menu" type="button" aria-label="打开导航" @click="menuOpen = true"><Menu :size="21" /></button><div><span>农户工作台</span><strong>{{ title }}</strong></div><p><span>{{ displayName.slice(0, 1).toUpperCase() }}</span>{{ displayName }}</p></header><RouterView /></section>
  </div>
</template>
