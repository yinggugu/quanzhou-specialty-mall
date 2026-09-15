<template>
  <!-- 头部导航 - 1:1复刻原JSP header结构 -->
  <header ref="headerEl">
    <div class="container">
      <nav class="navbar">
        <!-- Logo -->
        <router-link to="/" class="logo">
          泉州特产商城
        </router-link>

        <!-- 导航链接 -->
        <ul class="nav-links">
          <li>
            <router-link to="/" :class="{ active: $route.path === '/' }">
              首页
            </router-link>
          </li>
          <li>
            <router-link to="/products" :class="{ active: isActive('/products') }">
              商品列表
            </router-link>
          </li>
          <li>
            <router-link to="/cart" :class="{ active: $route.path === '/cart' }">
              购物车
            </router-link>
          </li>
          <li>
            <router-link to="/orders" :class="{ active: isActive('/orders') }">
              我的订单
            </router-link>
          </li>
          <li>
            <router-link to="/quanzhou" :class="{ active: isActive('/quanzhou') }">
              泉州文化
            </router-link>
          </li>
        </ul>

        <!-- 用户认证区域 - 登录/未登录区分显示 -->
        <div class="auth-links">
          <!-- 未登录 -->
          <template v-if="!isLoggedIn">
            <router-link to="/login" class="btn btn-secondary">登录</router-link>
            <router-link to="/register" class="btn btn-primary">注册</router-link>
          </template>

          <!-- 已登录 -->
          <template v-if="isLoggedIn">
            <span>欢迎您，{{ username }}</span>
            <a href="javascript:void(0)" class="btn btn-secondary" @click="handleLogout">退出</a>
          </template>
        </div>
      </nav>
    </div>
  </header>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'AppHeader',

  computed: {
    ...mapGetters(['isLoggedIn', 'username'])
  },

  mounted() {
    // 导航栏滚动效果 (对应原 animations.js 中的 initNavScrollEffect)
    this.initNavScroll()
  },

  methods: {
    ...mapActions(['logout']),

    // 判断导航链接是否激活（支持子路径匹配）
    isActive(basePath) {
      if (basePath === '/products') {
        return this.$route.path.startsWith('/products')
      }
      if (basePath === '/orders') {
        return this.$route.path.startsWith('/orders')
      }
      if (basePath === '/quanzhou') {
        return this.$route.path.startsWith('/quanzhou')
      }
      return this.$route.path === basePath
    },

    // 退出登录
    async handleLogout() {
      try {
        await this.logout()
        this.$message.success('已退出登录')
        this.$router.push('/')
      } catch (e) {
        // 即使API调用失败也清空本地状态
        this.$store.commit('LOGOUT')
        this.$router.push('/')
      }
    },

    // 导航栏滚动效果 (与 animations.js 中的逻辑一致)
    initNavScroll() {
      const header = this.$refs.headerEl
      if (!header) return

      let lastScrollTop = 0
      const scrollThreshold = 50

      window.addEventListener('scroll', () => {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop

        if (scrollTop > scrollThreshold) {
          header.classList.add('nav-scrolled')
        } else {
          header.classList.remove('nav-scrolled')
        }

        lastScrollTop = scrollTop
      })
    }
  }
}
</script>
