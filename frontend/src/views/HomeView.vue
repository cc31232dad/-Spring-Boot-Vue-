<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loadError = ref('')

onMounted(async () => {
  try {
    await auth.loadCurrentUser()
  } catch {
    loadError.value = '暂时无法读取账号信息，请稍后刷新。'
  }
})

async function logout() {
  auth.clearSession()
  await router.replace('/login')
}
</script>

<template>
  <main class="home-page">
    <header class="home-header">
      <div>
        <p class="eyebrow">AGRICULTURAL ASSISTANCE MARKET</p>
        <h1>助农商城</h1>
      </div>
      <button class="secondary-button" type="button" @click="logout">退出登录</button>
    </header>

    <section class="welcome-panel" aria-labelledby="welcome-title">
      <p class="eyebrow">今日田野来信</p>
      <h2 id="welcome-title">{{ auth.username ? `你好，${auth.username}` : '你好，欢迎来到助农商城' }}</h2>
      <p>一份来自土地的收成，也是一份校园里的温暖选择。</p>
      <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
    </section>
  </main>
</template>
