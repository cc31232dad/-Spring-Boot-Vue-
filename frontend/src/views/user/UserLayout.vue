<script setup lang="ts">
import { useAuthStore } from '../../stores/auth'
const auth = useAuthStore()
</script>
<template><main class="user-page"><div class="user-shell"><aside class="user-sidebar"><div class="user-profile-mini"><span class="avatar-placeholder">{{ (auth.username || 'U').slice(0, 1).toUpperCase() }}</span><strong>{{ auth.username || 'User' }}</strong></div><nav><RouterLink :to="{ name: 'user' }">Overview</RouterLink><RouterLink :to="{ name: 'user-orders' }">Orders</RouterLink><RouterLink :to="{ name: 'user-address' }">Addresses</RouterLink><RouterLink :to="{ name: 'user-favorites' }">Favorites</RouterLink><RouterLink :to="{ name: 'user-settings' }">Settings</RouterLink></nav></aside><section class="user-content"><RouterView /></section></div></main></template>
