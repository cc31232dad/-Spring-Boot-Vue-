import { listFarmerApplications } from '../../api/farmers'
import { listAdminOrders, type Order } from '../../api/orders'
import { listAdminProducts } from '../../api/products'

type AdminDashboardError = 'pendingProducts' | 'onSaleProducts' | 'pendingFarmers' | 'orders'

export interface AdminDashboardData {
  stats: { pendingProducts: number | null; onSaleProducts: number | null; pendingFarmers: number | null; orders: number | null }
  recentOrders: Order[]
  errors: AdminDashboardError[]
}

export async function loadAdminDashboard(): Promise<AdminDashboardData> {
  const results = await Promise.allSettled([
    listAdminProducts('PENDING_REVIEW'),
    listAdminProducts('ON_SALE'),
    listFarmerApplications(),
    listAdminOrders()
  ])
  const keys: AdminDashboardError[] = ['pendingProducts', 'onSaleProducts', 'pendingFarmers', 'orders']
  const errors = results.flatMap((result, index) => result.status === 'rejected' ? [keys[index]] : [])
  const count = (index: number) => results[index].status === 'fulfilled' ? results[index].value.length : null
  const orders = results[3].status === 'fulfilled' ? results[3].value as Order[] : []

  return {
    stats: {
      pendingProducts: count(0),
      onSaleProducts: count(1),
      pendingFarmers: count(2),
      orders: count(3)
    },
    recentOrders: [...orders].sort((a, b) => b.id - a.id).slice(0, 5),
    errors,
  }
}
