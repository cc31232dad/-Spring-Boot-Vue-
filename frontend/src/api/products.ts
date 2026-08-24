import http from './http'

export interface Category {
  id: number
  name: string
}

export type ProductStatus = 'PENDING_REVIEW' | 'ON_SALE' | 'OFF_SALE' | 'REJECTED'

export interface ProductSummary {
  id: number
  categoryId: number
  categoryName: string
  name: string
  price: number
  stock: number
  originPlace: string
  imageUrl: string
}

export interface ProductDetail extends ProductSummary {
  farmerId: number
  description: string
  status: ProductStatus
  reviewedBy?: number
  reviewedAt?: string
  reviewReason?: string
}

export interface ProductPayload {
  categoryId: number
  name: string
  description: string
  price: number
  stock: number
  originPlace: string
  imageUrl: string
}

export interface ProductQuery {
  keyword?: string
  categoryId?: number
}

export async function listCategories() {
  const { data } = await http.get('/categories')
  return data.data as Category[]
}

export async function listProducts(params: ProductQuery = {}) {
  const { data } = await http.get('/products', { params })
  return data.data as ProductSummary[]
}

export async function getProduct(id: number) {
  const { data } = await http.get(`/products/${id}`)
  return data.data as ProductDetail
}

export async function createProduct(payload: ProductPayload) {
  const { data } = await http.post('/farmer/products', payload)
  return data.data as ProductDetail
}

export async function updateProduct(id: number, payload: ProductPayload) {
  const { data } = await http.put(`/farmer/products/${id}`, payload)
  return data.data as ProductDetail
}

export async function listAdminProducts(status?: ProductStatus) {
  const { data } = await http.get('/admin/products/review', { params: { status } })
  return data.data as ProductDetail[]
}

export async function uploadProductImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post('/farmer/product-images', formData)
  return data.data.url as string
}

export async function approveProduct(id: number) {
  const { data } = await http.post(`/admin/products/${id}/approve`)
  return data.data as ProductDetail
}

export async function rejectProduct(id: number, reason: string) {
  const { data } = await http.post(`/admin/products/${id}/reject`, { reason })
  return data.data as ProductDetail
}

export async function listFarmerProducts() {
  const { data } = await http.get('/farmer/products')
  return data.data as ProductDetail[]
}

export async function resubmitProduct(id: number) {
  const { data } = await http.patch(`/farmer/products/${id}/on-sale`)
  return data.data as ProductDetail
}

export async function takeProductOffSale(id: number) {
  const { data } = await http.patch(`/farmer/products/${id}/off-sale`)
  return data.data as ProductDetail
}
