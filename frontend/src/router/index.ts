import { createMemoryHistory, createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import HomeView from '../views/HomeView.vue'
import ProductDetailView from '../views/ProductDetailView.vue'
import LoginView from '../views/auth/LoginView.vue'
import RegisterView from '../views/auth/RegisterView.vue'
import ProductFormView from '../views/farmer/ProductFormView.vue'
import CartView from '../views/CartView.vue'
import OrderListView from '../views/OrderListView.vue'
import OrderDetailView from '../views/OrderDetailView.vue'
import FarmerOrdersView from '../views/farmer/FarmerOrdersView.vue'
import AdminOrdersView from '../views/admin/AdminOrdersView.vue'
import AdminProductsView from '../views/admin/AdminProductsView.vue'
import AdminFarmersView from '../views/admin/AdminFarmersView.vue'
import FarmerProductsView from '../views/farmer/FarmerProductsView.vue'
import SeckillView from '../views/SeckillView.vue'
import UserLayout from '../views/user/UserLayout.vue'
import UserOverviewView from '../views/user/UserOverviewView.vue'
import UserOrdersView from '../views/user/UserOrdersView.vue'
import UserAddressView from '../views/user/UserAddressView.vue'
import UserFavoritesView from '../views/user/UserFavoritesView.vue'
import UserSettingsView from '../views/user/UserSettingsView.vue'

const router = createRouter({
  history: typeof window === 'undefined' ? createMemoryHistory() : createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/products/:id', name: 'product-detail', component: ProductDetailView },
    { path: '/cart', name: 'cart', component: CartView, meta: { requiresAuth: true } },
    { path: '/orders', name: 'orders', component: OrderListView, meta: { requiresAuth: true } },
    { path: '/orders/:id', name: 'order-detail', component: OrderDetailView, meta: { requiresAuth: true } },
    { path: '/seckill', name: 'seckill', component: SeckillView },
    { path: '/user', component: UserLayout, meta: { requiresAuth: true }, children: [
      { path: '', name: 'user', component: UserOverviewView },
      { path: 'orders', name: 'user-orders', component: UserOrdersView },
      { path: 'address', name: 'user-address', component: UserAddressView },
      { path: 'favorites', name: 'user-favorites', component: UserFavoritesView },
      { path: 'settings', name: 'user-settings', component: UserSettingsView }
    ] },
    { path: '/farmer/orders', name: 'farmer-orders', component: FarmerOrdersView, meta: { requiresAuth: true } },
    { path: '/admin/orders', name: 'admin-orders', component: AdminOrdersView, meta: { requiresAuth: true } },
    { path: '/admin/products', name: 'admin-products', component: AdminProductsView, meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
    { path: '/admin/farmers', name: 'admin-farmers', component: AdminFarmersView, meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
    { path: '/farmer/products', name: 'farmer-products', component: FarmerProductsView, meta: { requiresAuth: true, requiredRole: 'FARMER' } },
    { path: '/farmer/products/:id/edit', name: 'farmer-product-edit', component: ProductFormView, meta: { requiresAuth: true, requiredRole: 'FARMER' } },
    {
      path: '/farmer/products/new',
      name: 'farmer-product-new',
      component: ProductFormView,
      meta: { requiresAuth: true }
    },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView }
  ]
})

router.beforeEach(async (to) => {
  if (to.meta.requiresAuth && !useAuthStore().isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const requiredRole = to.meta.requiredRole as string | undefined
  if (requiredRole) {
    const auth = useAuthStore()
    if (!auth.roles.length) {
      try { await auth.loadCurrentUser() } catch { return { name: 'login', query: { redirect: to.fullPath } } }
    }
    if (!auth.roles.includes(requiredRole)) return { name: 'home' }
  }

  if ((to.name === 'login' || to.name === 'register') && useAuthStore().isLoggedIn) {
    return { name: 'home' }
  }
})

export default router
