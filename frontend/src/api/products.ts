import http from './http'

export interface Category {
  id: number
  name: string
}

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
  status: 'ON_SALE' | 'OFF_SALE'
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
