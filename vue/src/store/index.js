// Vuex Store - 用户状态管理
// ✅ 双表拆分：用 type 字段（"user"/"admin"）替代旧 role 字段区分身份
import Vue from 'vue'
import Vuex from 'vuex'
import { userLogin as userLoginApi, adminLogin as adminLoginApi, register as registerApi, parseToken } from '@/api/auth'
import { getCart } from '@/api/cart'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    // 用户信息（从 localStorage 恢复）
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    // JWT token
    token: localStorage.getItem('token') || '',
    // 购物车商品数量
    cartCount: 0
  },

  getters: {
    // 是否已登录
    isLoggedIn: state => !!state.token,
    // 当前用户名
    username: state => state.user?.username || '',
    // ✅ 是否是管理员（通过 JWT type 字段判断）
    isAdmin: state => {
      if (!state.token) return false
      const info = parseToken(state.token)
      return info?.type === 'admin'
    },
    // ✅ 当前用户身份类型
    userType: state => {
      if (!state.token) return null
      const info = parseToken(state.token)
      return info?.type || null
    }
  },

  mutations: {
    SET_USER(state, user) {
      state.user = user
      if (user) {
        localStorage.setItem('user', JSON.stringify(user))
      } else {
        localStorage.removeItem('user')
      }
    },
    SET_TOKEN(state, token) {
      state.token = token
      if (token) {
        localStorage.setItem('token', token)
      } else {
        localStorage.removeItem('token')
      }
    },
    SET_CART_COUNT(state, count) {
      state.cartCount = count
    },
    LOGOUT(state) {
      state.user = null
      state.token = ''
      state.cartCount = 0
      localStorage.removeItem('user')
      localStorage.removeItem('token')
    }
  },

  actions: {
    // ✅ 前台用户登录
    async userLogin({ commit }, { username, password }) {
      const res = await userLoginApi({ username, password })
      if (res.code === 200) {
        commit('SET_TOKEN', res.data.token)
        commit('SET_USER', res.data.user)
        return res
      }
      throw new Error(res.message || '登录失败')
    },

    // ✅ 后台管理员登录
    async adminLogin({ commit }, { username, password }) {
      const res = await adminLoginApi({ username, password })
      if (res.code === 200) {
        commit('SET_TOKEN', res.data.token)
        commit('SET_USER', res.data.user)
        return res
      }
      throw new Error(res.message || '登录失败')
    },

    // 注册
    async register({ commit }, formData) {
      const res = await registerApi(formData)
      return res
    },

    // 退出登录
    async logout({ commit }) {
      commit('LOGOUT')
    },

    // 获取购物车数量
    async fetchCartCount({ commit, state }) {
      if (!state.token) return
      try {
        const res = await getCart()
        if (res.code === 200 && res.data) {
          const count = res.data.items
            ? res.data.items.reduce((sum, item) => sum + item.quantity, 0)
            : 0
          commit('SET_CART_COUNT', count)
        }
      } catch (e) {
        // 静默处理
      }
    }
  }
})
