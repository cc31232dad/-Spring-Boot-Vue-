import http from './http'
import type { Order } from './orders'

export type Address = { id: number; name: string; phone: string; province: string; city: string; district: string; detail: string; isDefault: boolean }
export type Favorite = { id: number; productId: number; name: string; price: number; stock: number; originPlace: string; imageUrl: string; createdAt: string }
export type Profile = { id: number; username: string; nickname: string | null; realName: string | null; phone: string; email: string | null; avatarUrl: string | null }
export const selectCheckoutAddress = (addresses: Address[]) => addresses.find((item) => item.isDefault) ?? addresses[0]
export const formatAddress = (address: Address) => [address.province, address.city, address.district, address.detail].join('')
export const listOrders = async (status?: string) => (await http.get('/orders/my', { params: status ? { status } : {} })).data.data as Order[]
export const cancelOrder = async (id: number) => (await http.post(`/orders/${id}/cancel`)).data.data as Order
export const confirmOrder = async (id: number) => (await http.post(`/orders/${id}/confirm`)).data.data as Order
export const listAddresses = async () => (await http.get('/address')).data.data as Address[]
export const saveAddress = async (payload: Omit<Address, 'id' | 'isDefault'>, id?: number) => (await (id ? http.put(`/address/${id}`, payload) : http.post('/address', payload))).data.data as Address
export const deleteAddress = async (id: number) => { await http.delete(`/address/${id}`) }
export const setDefaultAddress = async (id: number) => (await http.put(`/address/${id}/default`)).data.data as Address
export const addFavorite = async (productId: number) => (await http.post('/favorites/' + productId)).data.data as Favorite
export const listFavorites = async () => (await http.get('/favorites')).data.data as Favorite[]
export const removeFavorite = async (productId: number) => { await http.delete(`/favorites/${productId}`) }
export const getProfile = async () => (await http.get('/user/profile')).data.data as Profile
export const updateProfile = async (payload: Partial<Profile>) => (await http.put('/user/profile', payload)).data.data as Profile
