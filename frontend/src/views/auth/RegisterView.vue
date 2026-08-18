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
    const message = errorMessage(reason)
    error.value = message === 'Validation error'
      ? '注册信息不符合规则，请检查用户名、手机号和密码。'
      : message
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
          <input v-model.trim="form.username" aria-describedby="username-rule" autocomplete="username" minlength="4" maxlength="32" pattern="[A-Za-z0-9_]{4,32}" required />
          <span id="username-rule" class="field-help">4～32 位，只能使用字母、数字和下划线</span>
        </label>
        <label>
          手机号
          <input v-model.trim="form.phone" aria-describedby="phone-rule" inputmode="numeric" autocomplete="tel" pattern="\d{11}" required />
          <span id="phone-rule" class="field-help">请输入 11 位数字</span>
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" aria-describedby="password-rule" autocomplete="new-password" minlength="8" maxlength="64" pattern="(?=.*[A-Za-z])(?=.*\d).{8,64}" required />
          <span id="password-rule" class="field-help">8～64 位，必须同时包含字母和数字</span>
        </label>
        <label>
          确认密码
          <input v-model="form.confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="64" required />
        </label>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button type="submit" :disabled="submitting">{{ submitting ? '正在创建…' : '创建账号' }}</button>
      </form>

      <p class="auth-switch">已有账号？<RouterLink to="/login">去登录</RouterLink></p>
    </section>
  </main>
</template>
