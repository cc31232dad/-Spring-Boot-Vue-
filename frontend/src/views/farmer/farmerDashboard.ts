import { listFarmerOrders, type Order } from '../../api/orders'
import { listFarmerProducts } from '../../api/products'

export interface FarmerDashboardData {
  products: { total: number | null; pending: number | null; onSale: number | null; rejected: number | null }
  orders: { total: number | null; pendingShipment: number | null }
  recentOrders: Order[]
  errors: Array<'products' | 'orders'>
}

export async function loadFarmerDashboard(): Promise<FarmerDashboardData> {
  const [productsResult, ordersResult] = await Promise.allSettled([
    listFarmerProducts(),
    listFarmerOrders()
  ])
  const products = productsResult.status === 'fulfilled' ? productsResult.value : null
  const orders = ordersResult.status === 'fulfilled' ? ordersResult.value : null
  const errors: FarmerDashboardData['errors'] = []
  if (!products) errors.push('products')
  if (!orders) errors.push('orders')

  return {
    products: {
      total: products?.length ?? null,
      pending: products?.filter((item) => item.status === 'PENDING_REVIEW').length ?? null,
      onSale: products?.filter((item) => item.status === 'ON_SALE').length ?? null,
      rejected: products?.filter((item) => item.status === 'REJECTED').length ?? null
    },
    orders: {
      total: orders?.length ?? null,
      pendingShipment: orders?.filter((item) => item.status === 'PENDING_SHIPMENT').length ?? null
    },
    recentOrders: [...(orders ?? [])].sort((a, b) => b.id - a.id).slice(0, 5),
    errors
  }
}
