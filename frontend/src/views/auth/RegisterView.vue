<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { errorMessage, register } from '../../api/auth'

const router = useRouter()
const form = reactive({ username: '', phone: '', password: '', confirmPassword: '' })
const error = ref('')
const submitting = ref(false)

async function submit() {
  error.value = ''
  if (form.password !== form.confirmPassword) {
    error.value = '两次输入的密码不一致。'
    return
  }

  submitting.value = true
  try {
    await register({ username: form.username, phone: form.phone, password: form.password })
    await router.replace('/login')
  } catch (reason) {
    error.value = errorMessage(reason)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card" aria-labelledby="register-title">
      <p class="eyebrow">把好生活带回校园</p>
      <h1 id="register-title">加入助农商城</h1>
      <p class="intro">创建账号，和我们一起支持田野里的好收成。</p>

      <form class="auth-form" @submit.prevent="submit">
        <label>
          用户名
          <input v-model.trim="form.username" autocomplete="username" minlength="4" maxlength="32" required />
        </label>
        <label>
          手机号
          <input v-model.trim="form.phone" inputmode="numeric" autocomplete="tel" pattern="\d{11}" required />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" autocomplete="new-password" minlength="8" required />
        </label>
        <label>
          确认密码
          <input v-model="form.confirmPassword" type="password" autocomplete="new-password" minlength="8" required />
        </label>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button type="submit" :disabled="submitting">{{ submitting ? '正在创建…' : '创建账号' }}</button>
      </form>

      <p class="auth-switch">已有账号？<RouterLink to="/login">去登录</RouterLink></p>
    </section>
  </main>
</template>
