import http from './http'

export type OrderStatus = 'PENDING_PAYMENT' | 'PENDING_SHIPMENT' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED'

export interface CheckoutPayload {
  cartItemIds: number[]
  receiverName: string
  receiverPhone: string
  receiverAddress: string
}

export interface OrderItem {
  id: number
  productId: number
  productName: string
  productImageUrl: string
  originPlace: string
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface Order {
  id: number
  orderNo: string
  buyerId: number
  farmerId: number
  status: OrderStatus
  orderType: 'NORMAL' | 'SECKILL'
  totalAmount: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  items: OrderItem[]
}

export async function checkout(payload: CheckoutPayload): Promise<Order[]> {
  const { data } = await http.post('/orders/checkout', payload)
  return data.data as Order[]
}

export async function listMyOrders(): Promise<Order[]> {
  const { data } = await http.get('/orders/my')
  return data.data as Order[]
}

export async function getOrder(id: number): Promise<Order> {
  const { data } = await http.get(`/orders/${id}`)
  return data.data as Order
}

export async function cancelOrder(id: number): Promise<Order> {
  const { data } = await http.patch(`/orders/${id}/cancel`)
  return data.data as Order
}

export async function completeOrder(id: number): Promise<Order> {
  const { data } = await http.patch(`/orders/${id}/complete`)
  return data.data as Order
}

export async function listFarmerOrders(): Promise<Order[]> {
  const { data } = await http.get('/farmer/orders')
  return data.data as Order[]
}

export async function shipOrder(id: number): Promise<Order> {
  const { data } = await http.patch(`/farmer/orders/${id}/ship`)
  return data.data as Order
}

export async function listAdminOrders(): Promise<Order[]> {
  const { data } = await http.get('/admin/orders')
  return data.data as Order[]
}
