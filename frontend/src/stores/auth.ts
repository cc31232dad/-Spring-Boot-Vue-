import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '../api/auth'

const TOKEN_KEY = 'agromall.access-token'

function readToken() {
  return typeof localStorage === 'undefined' ? '' : (localStorage.getItem(TOKEN_KEY) ?? '')
}

function writeToken(token: string) {
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(TOKEN_KEY, token)
  }
}

function removeToken() {
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(readToken())
  const username = ref('')
  const roles = ref<string[]>([])
  const isLoggedIn = computed(() => Boolean(accessToken.value))

  async function login(credentials: authApi.LoginPayload) {
    const session = await authApi.login(credentials)
    accessToken.value = session.accessToken
    writeToken(session.accessToken)
  }

  async function loadCurrentUser() {
    const user = await authApi.me()
    username.value = user.username
    roles.value = user.roles
    return user
  }

  function clearSession() {
    accessToken.value = ''
    username.value = ''
    roles.value = []
    removeToken()
  }

  return { accessToken, username, roles, isLoggedIn, login, loadCurrentUser, clearSession }
})
