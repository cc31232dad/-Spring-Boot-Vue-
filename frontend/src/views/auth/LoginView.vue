<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { errorMessage } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const form = reactive({ username: '', password: '' })
const error = ref('')
const submitting = ref(false)

async function submit() {
  error.value = ''
  submitting.value = true

  try {
    await auth.login(form)
    await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
  } catch (reason) {
    error.value = errorMessage(reason)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card" aria-labelledby="login-title">
      <p class="eyebrow">田间好物 · 直连校园</p>
      <h1 id="login-title">回到助农商城</h1>
      <p class="intro">登录后，继续发现来自田野的新鲜心意。</p>

      <form class="auth-form" @submit.prevent="submit">
        <label>
          用户名
          <input v-model.trim="form.username" autocomplete="username" required />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" autocomplete="current-password" required />
        </label>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button type="submit" :disabled="submitting">{{ submitting ? '正在登录…' : '登录商城' }}</button>
      </form>

      <p class="auth-switch">还没有账号？<RouterLink to="/register">去注册</RouterLink></p>
    </section>
  </main>
</template>
