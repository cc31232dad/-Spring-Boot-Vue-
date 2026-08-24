<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { errorMessage } from '../../api/auth'
import { updatePassword, updatePhone } from '../../api/userCenter'
import { useAuthStore } from '../../stores/auth'
import { useUserCenterStore } from '../../stores/userCenter'
import { validatePasswordChange, validatePhoneChange } from './securityForms'

const router = useRouter()
const auth = useAuthStore()
const store = useUserCenterStore()
const profileForm = reactive({ nickname: '', realName: '', email: '' })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const phoneForm = reactive({ currentPassword: '', phone: '' })
const profileState = reactive({ busy: false, error: '', success: '' })
const passwordState = reactive({ busy: false, error: '' })
const phoneState = reactive({ busy: false, error: '', success: '' })
const loadError = ref('')
const profileLoading = ref(true)

onMounted(async () => {
  try {
    await store.loadProfile()
    if (store.profile) {
      Object.assign(profileForm, { nickname: store.profile.nickname || '', realName: store.profile.realName || '', email: store.profile.email || '' })
      phoneForm.phone = store.profile.phone
    }
  } catch (error) { loadError.value = errorMessage(error) }
})

async function saveProfile() {
  profileState.error = ''; profileState.success = ''; profileState.busy = true
  try { await store.saveProfile(profileForm); profileState.success = '基本资料已保存' }
  catch (error) { profileState.error = errorMessage(error) }
  finally { profileState.busy = false }
}

async function savePassword() {
  passwordState.error = validatePasswordChange(passwordForm.currentPassword, passwordForm.newPassword, passwordForm.confirmPassword)
  if (passwordState.error) return
  passwordState.busy = true
  try {
    await updatePassword({ currentPassword: passwordForm.currentPassword, newPassword: passwordForm.newPassword })
    auth.clearSession()
    await router.replace({ name: 'login', query: { passwordChanged: '1' } })
  } catch (error) { passwordState.error = errorMessage(error) }
  finally { passwordState.busy = false }
}

async function savePhone() {
  phoneState.error = validatePhoneChange(phoneForm.currentPassword, phoneForm.phone)
  phoneState.success = ''
  if (phoneState.error) return
  phoneState.busy = true
  try {
    await updatePhone({ currentPassword: phoneForm.currentPassword, phone: phoneForm.phone })
    await store.loadProfile()
    phoneForm.currentPassword = ''
    phoneState.success = '绑定手机号已更新'
  } catch (error) { phoneState.error = errorMessage(error) }
  finally { phoneState.busy = false }
}
</script>

<template>
  <div class="user-heading"><div><p class="eyebrow">账户管理</p><h1>账户设置</h1></div></div>
  <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
  <p v-else-if="profileLoading" class="muted-text" role="status">正在加载账户资料...</p>

  <form class="user-panel settings-card settings-form" @submit.prevent="saveProfile">
    <div><h2>基本信息</h2><p class="muted-text">完善公开资料，方便订单与售后联系。</p></div>
    <div class="avatar-placeholder large">{{ (store.profile?.username || '农').slice(0, 1).toUpperCase() }}</div>
    <div class="form-grid"><label>昵称<input v-model.trim="profileForm.nickname"></label><label>真实姓名<input v-model.trim="profileForm.realName"></label><label>当前手机号<input :value="store.profile?.phone" disabled></label><label>邮箱<input v-model.trim="profileForm.email" type="email"></label></div>
    <p v-if="profileState.error" class="form-error" role="alert">{{ profileState.error }}</p><p v-if="profileState.success" class="form-success" role="status">{{ profileState.success }}</p>
    <button type="submit" :disabled="profileLoading || profileState.busy">{{ profileState.busy ? '正在保存...' : '保存基本资料' }}</button>
  </form>

  <form class="user-panel settings-card settings-form" @submit.prevent="savePhone">
    <div><h2>修改绑定手机号</h2><p class="muted-text">修改前需要验证当前登录密码。</p></div>
    <div class="form-grid"><label>当前密码<input v-model="phoneForm.currentPassword" required type="password" autocomplete="current-password"></label><label>新手机号<input v-model.trim="phoneForm.phone" required type="tel" inputmode="numeric" maxlength="11" autocomplete="tel"></label></div>
    <p v-if="phoneState.error" class="form-error" role="alert">{{ phoneState.error }}</p><p v-if="phoneState.success" class="form-success" role="status">{{ phoneState.success }}</p>
    <button type="submit" :disabled="phoneState.busy">{{ phoneState.busy ? '正在更新...' : '更新手机号' }}</button>
  </form>

  <form class="user-panel settings-card settings-form" @submit.prevent="savePassword">
    <div><h2>修改登录密码</h2><p class="muted-text">新密码需为 8-64 位，并同时包含字母和数字。修改成功后需要重新登录。</p></div>
    <div class="form-grid security-password-grid"><label>当前密码<input v-model="passwordForm.currentPassword" required type="password" autocomplete="current-password"></label><label>新密码<input v-model="passwordForm.newPassword" required type="password" autocomplete="new-password"></label><label>确认新密码<input v-model="passwordForm.confirmPassword" required type="password" autocomplete="new-password"></label></div>
    <p v-if="passwordState.error" class="form-error" role="alert">{{ passwordState.error }}</p>
    <button type="submit" :disabled="passwordState.busy">{{ passwordState.busy ? '正在修改...' : '修改密码' }}</button>
  </form>
</template>

<style scoped>
.settings-form { display: grid; justify-items: start; gap: 16px; padding: 22px 0 28px; border-bottom: 1px solid #e1e9dc; }
.settings-form:last-child { border-bottom: 0; }
.settings-form .form-grid { width: 100%; margin: 0; }
.security-password-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.form-success { margin: 0; color: var(--leaf-green-deep); font-weight: 700; }
@media (max-width: 760px) { .security-password-grid { grid-template-columns: 1fr; } }
</style>
