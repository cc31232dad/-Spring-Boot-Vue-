<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useUserCenterStore } from '../../stores/userCenter'
const store = useUserCenterStore()
const form = ref({ nickname: '', realName: '', email: '' })
onMounted(async () => { await store.loadProfile(); if (store.profile) form.value = { nickname: store.profile.nickname || '', realName: store.profile.realName || '', email: store.profile.email || '' } })
async function save() { await store.saveProfile(form.value); alert('资料已保存') }
</script>
<template><div class="user-heading"><div><p class="eyebrow">账户管理</p><h1>账户设置</h1></div></div><section class="user-panel settings-card"><h2>基本信息</h2><div class="avatar-placeholder large">{{ (store.profile?.username || '农').slice(0, 1).toUpperCase() }}</div><div class="form-grid"><label>昵称<input v-model="form.nickname" /></label><label>真实姓名<input v-model="form.realName" /></label><label>手机号<input :value="store.profile?.phone" disabled /></label><label>邮箱<input v-model="form.email" /></label></div><button @click="save">保存修改</button></section><section class="user-panel settings-card"><h2>安全设置</h2><p class="muted-text">登录密码和绑定手机号可以在这里更新。</p></section></template>
