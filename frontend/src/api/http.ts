import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const http = axios.create({ baseURL: '/api' })

http.interceptors.request.use((config) => {
  const { accessToken } = useAuthStore()

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }

  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      useAuthStore().clearSession()
      if (router.currentRoute.value.name !== 'login') {
        await router.replace({ name: 'login' })
      }
    }

    return Promise.reject(error)
  }
)

export default http
