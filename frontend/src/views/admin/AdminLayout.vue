<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ClipboardCheck, LayoutDashboard, LogOut, Menu, PackageSearch, ShieldCheck, ShoppingBag, UsersRound, X } from '@lucide/vue'
import { useAuthStore } from '../../stores/auth'
const auth = useAuthStore(); const route = useRoute(); const router = useRouter(); const menuOpen = ref(false)
const titles: Record<string, string> = { 'admin-dashboard': '平台概览', 'admin-products': '商品审核', 'admin-farmers': '农户审核', 'admin-orders': '订单管理' }
const title = computed(() => titles[String(route.name)] ?? '管理后台'); const displayName = computed(() => auth.username || '管理员账号')
watch(() => route.fullPath, () => { menuOpen.value = false })
async function logout() { auth.clearSession(); await router.push({ name: 'login' }) }
</script>
<template>
  <div class="admin-shell workspace-shell">
    <button v-if="menuOpen" class="workspace-scrim" type="button" aria-label="关闭导航" @click="menuOpen = false"></button>
    <aside class="workspace-sidebar" :class="{ open: menuOpen }"><div class="workspace-brand"><span class="workspace-brand-mark"><ShieldCheck :size="22" /></span><div><strong>助农商城</strong><span>管理后台</span></div><button class="workspace-close" type="button" aria-label="关闭导航" @click="menuOpen = false"><X :size="20" /></button></div>
      <nav aria-label="管理后台导航"><RouterLink :to="{ name: 'admin-dashboard' }"><LayoutDashboard :size="19" /><span>工作台</span></RouterLink><RouterLink :to="{ name: 'admin-products' }"><PackageSearch :size="19" /><span>商品审核</span></RouterLink><RouterLink :to="{ name: 'admin-farmers' }"><UsersRound :size="19" /><span>农户审核</span></RouterLink><RouterLink :to="{ name: 'admin-orders' }"><ClipboardCheck :size="19" /><span>订单管理</span></RouterLink></nav>
      <div class="workspace-sidebar-footer"><RouterLink :to="{ name: 'home' }"><ShoppingBag :size="19" /><span>查看商城</span></RouterLink><button type="button" @click="logout"><LogOut :size="19" /><span>退出登录</span></button></div>
    </aside><section class="workspace-main"><header class="workspace-topbar"><button class="workspace-menu" type="button" aria-label="打开导航" @click="menuOpen = true"><Menu :size="21" /></button><div><span>助农商城管理后台</span><strong>{{ title }}</strong></div><p><span>{{ displayName.slice(0, 1).toUpperCase() }}</span>{{ displayName }}</p></header><RouterView /></section>
  </div>
</template>
