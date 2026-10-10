import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/Home.vue') },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'register', component: () => import('../views/Register.vue') },
  { path: '/upload', name: 'upload', component: () => import('../views/Upload.vue'), meta: { requiresAuth: true } },
  { path: '/video/:id', name: 'play', component: () => import('../views/Play.vue') },
  { path: '/category/:id', name: 'category', component: () => import('../views/Category.vue') },
  { path: '/me', name: 'me', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/user/:id', name: 'userProfile', component: () => import('../views/Profile.vue') },
  { path: '/message', name: 'message', component: () => import('../views/Message.vue'), meta: { requiresAuth: true } },
  { path: '/history', name: 'history', component: () => import('../views/History.vue'), meta: { requiresAuth: true } },
  { path: '/pm', name: 'pm', component: () => import('../views/PrivateMessage.vue'), meta: { requiresAuth: true } },
  { path: '/admin', name: 'admin', component: () => import('../views/Admin.vue'), meta: { requiresAuth: true } },
  { path: '/merchant', name: 'merchant', component: () => import('../views/Merchant.vue'), meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫：需要登录的页面未登录时跳转登录页
router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLogin) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
