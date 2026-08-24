import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../api/seckill'

export const useSeckillStore = defineStore('seckill', () => {
  const activities = ref<api.SeckillActivity[]>([])
  const loading = ref(false)
  const error = ref('')

  async function loadActivities() {
    loading.value = true
    error.value = ''
    try {
      activities.value = await api.listActivities()
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '秒杀活动暂时无法加载'
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function rush(id: number, payload: api.SeckillRushPayload) {
    return api.rush(id, payload)
  }

  return { activities, loading, error, loadActivities, rush }
})
