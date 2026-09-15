// Vue Router 路由配置 - 泉州特产商城
// ✅ 双表拆分：路由守卫用 JWT type 字段（"user"/"admin"）区分身份权限
import Vue from 'vue'
import VueRouter from 'vue-router'
import { parseToken } from '@/api/auth'

Vue.use(VueRouter)

// 路由懒加载 - 后台管理员页面
const AdminLayout = () => import('@/views/admin/AdminLayout.vue')
const AdminDashboard = () => import('@/views/admin/Dashboard.vue')
const AdminProductList = () => import('@/views/admin/ProductList.vue')
const AdminProductEdit = () => import('@/views/admin/ProductEdit.vue')
const AdminOrderList = () => import('@/views/admin/OrderList.vue')
const AdminOrderDetail = () => import('@/views/admin/OrderDetail.vue')
const AdminUserList = () => import('@/views/admin/UserList.vue')
const AdminUserEdit = () => import('@/views/admin/UserEdit.vue')
const AdminServiceChat = () => import('@/views/admin/ServiceChat.vue')
const AdminCommentList = () => import('@/views/admin/CommentList.vue')
const AdminSensitiveWord = () => import('@/views/admin/SensitiveWord.vue')
const AdminAfterSale = () => import('@/views/admin/AfterSale.vue')
const Home = () => import('@/views/Home.vue')
const Login = () => import('@/views/Login.vue')
const Register = () => import('@/views/Register.vue')
const ProductList = () => import('@/views/ProductList.vue')
const ProductDetail = () => import('@/views/ProductDetail.vue')
const Cart = () => import('@/views/Cart.vue')
const OrderList = () => import('@/views/OrderList.vue')
const OrderDetail = () => import('@/views/OrderDetail.vue')
const OrderReviewSelect = () => import('@/views/OrderReviewSelect.vue')
const OrderReviewWrite = () => import('@/views/OrderReviewWrite.vue')
const Checkout = () => import('@/views/Checkout.vue')
const Pay = () => import('@/views/Pay.vue')
const QuanzhouIntro = () => import('@/views/QuanzhouIntro.vue')
const QuanzhouFood = () => import('@/views/QuanzhouFood.vue')
const QuanzhouCraft = () => import('@/views/QuanzhouCraft.vue')
const Contact = () => import('@/views/Contact.vue')
const UserServiceChat = () => import('@/views/UserServiceChat.vue')

/**
 * 从 localStorage token 中解析用户类型
 * @returns {"user"|"admin"|null}
 */
function getUserType() {
  const token = localStorage.getItem('token')
  if (!token) return null
  const info = parseToken(token)
  return info?.type || null
}

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/login', name: 'Login', component: Login, meta: { guest: true } },
  { path: '/register', name: 'Register', component: Register, meta: { guest: true } },
  { path: '/products', name: 'ProductList', component: ProductList },
  { path: '/products/:id', name: 'ProductDetail', component: ProductDetail },
  { path: '/cart', name: 'Cart', component: Cart, meta: { requiresAuth: true } },
  { path: '/orders', name: 'OrderList', component: OrderList, meta: { requiresAuth: true } },
  { path: '/orders/:id', name: 'OrderDetail', component: OrderDetail, meta: { requiresAuth: true } },
  { path: '/orders/:id/review', name: 'OrderReviewSelect', component: OrderReviewSelect, meta: { requiresAuth: true } },
  { path: '/orders/:id/review/write', name: 'OrderReviewWrite', component: OrderReviewWrite, meta: { requiresAuth: true } },
  { path: '/checkout', name: 'Checkout', component: Checkout, meta: { requiresAuth: true } },
  { path: '/pay/:id', name: 'Pay', component: Pay, meta: { requiresAuth: true } },
  { path: '/quanzhou', name: 'QuanzhouIntro', component: QuanzhouIntro },
  { path: '/quanzhou/food', name: 'QuanzhouFood', component: QuanzhouFood },
  { path: '/quanzhou/craft', name: 'QuanzhouCraft', component: QuanzhouCraft },
  { path: '/contact', name: 'Contact', component: Contact },
  { path: '/service', name: 'UserServiceChat', component: UserServiceChat, meta: { requiresAuth: true } },
  { path: '/ai', name: 'AiChat', component: () => import('@/views/AiChat.vue') },
  { path: '/afterSaleDetail', name: 'AfterSaleDetail', component: () => import('@/views/AfterSaleDetail.vue'), meta: { requiresAuth: true } },
  // 管理员后台路由（嵌套在 AdminLayout 下）
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', name: 'AdminDashboard', component: AdminDashboard },
      { path: 'products', name: 'AdminProductList', component: AdminProductList },
      { path: 'products/add', name: 'AdminProductAdd', component: AdminProductEdit },
      { path: 'products/:id/edit', name: 'AdminProductEdit', component: AdminProductEdit },
      { path: 'orders', name: 'AdminOrderList', component: AdminOrderList },
      { path: 'orders/:id', name: 'AdminOrderDetail', component: AdminOrderDetail },
      { path: 'users', name: 'AdminUserList', component: AdminUserList },
      { path: 'users/:id/edit', name: 'AdminUserEdit', component: AdminUserEdit },
      { path: 'service', name: 'AdminServiceChat', component: AdminServiceChat },
      { path: 'comments', name: 'AdminCommentList', component: AdminCommentList },
      { path: 'sensitive', name: 'AdminSensitiveWord', component: AdminSensitiveWord },
      { path: 'afterSale', name: 'AdminAfterSale', component: AdminAfterSale }
    ]
  },
  // 404 重定向到首页
  { path: '*', redirect: '/' }
]

// 全局静默 Vue Router 导航冗余报错
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => err)
}

const router = new VueRouter({
  mode: 'hash',
  routes,
  scrollBehavior() {
    return { x: 0, y: 0 }
  }
})

// ✅ 路由守卫 - 基于 JWT type 字段的权限校验
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userType = getUserType()

  // 需要管理员权限的页面 (meta.requiresAdmin)
  if (to.matched.some(record => record.meta.requiresAdmin)) {
    if (!token) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
    } else if (userType !== 'admin') {
      // ✅ 非管理员 token 无法进入后台
      next({ name: 'Home' })
    } else {
      next()
    }
  }
  // 需要登录的页面 (meta.requiresAuth)
  else if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
    } else if (userType === 'admin') {
      // ✅ 管理员 token 无法访问前台/service 等需要登录的页面
      // 自动跳转到后台管理首页
      next({ name: 'AdminDashboard' })
    } else {
      next()
    }
  }
  // 已登录用户不允许访问登录/注册页
  else if (to.matched.some(record => record.meta.guest)) {
    if (token) {
      if (userType === 'admin') {
        next({ name: 'AdminDashboard' })
      } else {
        next({ name: 'Home' })
      }
    } else {
      next()
    }
  }
  else {
    next()
  }
})

export default router
