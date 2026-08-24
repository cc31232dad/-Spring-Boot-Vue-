import http from './http'

export interface CartItem {
  id: number
  productId: number
  productName: string
  price: number
  quantity: number
  stock: number
  originPlace: string
  imageUrl: string
  farmerId: number
  subtotal: number
}

export interface Cart {
  items: CartItem[]
  totalAmount: number
}

export interface AddCartItemPayload {
  productId: number
  quantity: number
}

export interface UpdateCartItemPayload {
  quantity: number
}

export async function getCart(): Promise<Cart> {
  const { data } = await http.get('/cart')
  return data.data as Cart
}

export async function addCartItem(payload: AddCartItemPayload): Promise<Cart> {
  const { data } = await http.post('/cart/items', payload)
  return data.data as Cart
}

export async function updateCartItem(id: number, payload: UpdateCartItemPayload): Promise<Cart> {
  const { data } = await http.put(`/cart/items/${id}`, payload)
  return data.data as Cart
}

export async function deleteCartItem(id: number): Promise<void> {
  await http.delete(`/cart/items/${id}`)
}

export async function clearCart(): Promise<void> {
  await http.delete('/cart')
}
