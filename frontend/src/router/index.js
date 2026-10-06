import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
    },
    {
      path: '/cart',
      name: 'cart',
      component: () => import('../views/CartView.vue'),
    },
    {
      path: '/orders',
      name: 'orders',
      meta: { auth: true },
      component: () => import('../views/OrdersView.vue'),
    },
    {
      path: '/account',
      name: 'account',
      meta: { auth: true },
      component: () => import('../views/AccountView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.auth && !localStorage.getItem('token')) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router
