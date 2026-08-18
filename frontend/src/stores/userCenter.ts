import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../api/userCenter'

export const useUserCenterStore = defineStore('user-center', () => {
  const addresses = ref<api.Address[]>([])
  const favorites = ref<api.Favorite[]>([])
  const profile = ref<api.Profile | null>(null)
  const loading = ref(false)
  async function loadAddresses() { loading.value = true; try { addresses.value = await api.listAddresses() } finally { loading.value = false } }
  async function saveAddress(payload: Omit<api.Address, 'id' | 'isDefault'>, id?: number) { const saved = await api.saveAddress(payload, id); await loadAddresses(); return saved }
  async function removeAddress(id: number) { await api.deleteAddress(id); addresses.value = addresses.value.filter((item) => item.id !== id) }
  async function makeDefault(id: number) { await api.setDefaultAddress(id); await loadAddresses() }
  async function loadFavorites() { favorites.value = await api.listFavorites() }
  async function removeFavorite(productId: number) { await api.removeFavorite(productId); favorites.value = favorites.value.filter((item) => item.productId !== productId) }
  async function loadProfile() { profile.value = await api.getProfile() }
  async function saveProfile(payload: Partial<api.Profile>) { profile.value = await api.updateProfile(payload) }
  return { addresses, favorites, profile, loading, loadAddresses, saveAddress, removeAddress, makeDefault, loadFavorites, removeFavorite, loadProfile, saveProfile }
})
