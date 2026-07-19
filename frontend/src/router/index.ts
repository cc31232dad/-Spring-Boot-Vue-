import { createMemoryHistory, createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/auth/LoginView.vue'
import RegisterView from '../views/auth/RegisterView.vue'

const router = createRouter({
  history: typeof window === 'undefined' ? createMemoryHistory() : createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    // Temporary resolution target for catalog cards; Task 6 replaces this with the detail view.
    { path: '/products/:id', name: 'product-detail', redirect: { name: 'home' } },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView }
  ]
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !useAuthStore().isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if ((to.name === 'login' || to.name === 'register') && useAuthStore().isLoggedIn) {
    return { name: 'home' }
  }
})

export default router
