<template>
  <div class="admin-layout">
    <!-- 顶部栏 -->
    <header class="admin-topbar">
      <div class="topbar-left">
        <h1 class="topbar-logo">泉州特产商城 <span class="topbar-sub">后台管理</span></h1>
      </div>
      <div class="topbar-right">
        <span class="topbar-user">欢迎您，{{ username }}</span>
        <button class="topbar-logout" @click="handleLogout">退出</button>
      </div>
    </header>

    <div class="admin-body">
      <!-- 侧边导航 -->
      <aside class="admin-sidebar">
        <nav class="sidebar-nav">
          <router-link to="/admin" exact class="nav-item" :class="{ active: isExact('/admin') }">
            <i class="el-icon-s-data nav-icon"></i>
            <span class="nav-label">仪表盘</span>
          </router-link>
          <router-link to="/admin/products" class="nav-item" :class="{ active: isActive('/admin/products') }">
            <i class="el-icon-s-goods nav-icon"></i>
            <span class="nav-label">商品管理</span>
          </router-link>
          <router-link to="/admin/orders" class="nav-item" :class="{ active: isActive('/admin/orders') }">
            <i class="el-icon-s-order nav-icon"></i>
            <span class="nav-label">订单管理</span>
          </router-link>
          <!-- ✅ 评价管理子菜单 -->
          <!-- ✅ 售后管理子菜单 -->
          <div class="nav-submenu" :class="{ open: isActive('/admin/afterSale') }">
            <div class="nav-item nav-submenu-toggle" @click="toggleAfterSaleMenu">
              <i class="el-icon-s-claim nav-icon"></i>
              <span class="nav-label">售后管理</span>
              <span class="nav-arrow">{{ afterSaleMenuOpen ? '▾' : '▸' }}</span>
            </div>
            <div v-show="afterSaleMenuOpen" class="nav-submenu-items">
              <router-link to="/admin/afterSale" class="nav-sub-item" :class="{ active: isActive('/admin/afterSale') }">售后列表</router-link>
            </div>
          </div>
          <div class="nav-submenu" :class="{ open: isActive('/admin/comments') || isActive('/admin/sensitive') }">
            <div class="nav-item nav-submenu-toggle" @click="toggleCommentMenu">
              <i class="el-icon-chat-line-square nav-icon"></i>
              <span class="nav-label">评价管理</span>
              <span class="nav-arrow">{{ commentMenuOpen ? '▾' : '▸' }}</span>
            </div>
            <div v-show="commentMenuOpen" class="nav-submenu-items">
              <router-link to="/admin/comments" class="nav-sub-item" :class="{ active: isActive('/admin/comments') }">评价列表</router-link>
              <router-link to="/admin/sensitive" class="nav-sub-item" :class="{ active: isActive('/admin/sensitive') }">敏感词管理</router-link>
            </div>
          </div>
          <router-link to="/admin/users" class="nav-item" :class="{ active: isActive('/admin/users') }">
            <i class="el-icon-user nav-icon"></i>
            <span class="nav-label">用户管理</span>
          </router-link>
          <router-link to="/admin/service" class="nav-item" :class="{ active: isActive('/admin/service') }">
            <i class="el-icon-s-custom nav-icon"></i>
            <span class="nav-label">客服管理</span>
          </router-link>
        </nav>
      </aside>

      <!-- 内容区 -->
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'AdminLayout',
  data() {
    return { commentMenuOpen: false, afterSaleMenuOpen: false }
  },
  computed: {
    ...mapGetters(['username'])
  },
  methods: {
    toggleCommentMenu() { this.commentMenuOpen = !this.commentMenuOpen },
    toggleAfterSaleMenu() { this.afterSaleMenuOpen = !this.afterSaleMenuOpen },
    ...mapActions(['logout']),
    isActive(path) {
      return this.$route.path.startsWith(path)
    },
    isExact(path) {
      return this.$route.path === path
    },
    async handleLogout() {
      try {
        await this.logout()
        this.$message.success('已退出登录')
        this.$router.push('/')
      } catch {
        this.$store.commit('LOGOUT')
        this.$router.push('/')
      }
    }
  }
}
</script>

<style scoped>
/* ========================================
   全局布局
   ======================================== */
.admin-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f7f3ed;
}

/* ========================================
   顶部栏
   ======================================== */
.admin-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 52px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #ede6dc;
  flex-shrink: 0;
}
.topbar-logo {
  font-size: 16px;
  font-weight: 600;
  color: #3d2b1a;
  margin: 0;
}
.topbar-sub {
  font-weight: 400;
  color: #b0957a;
  margin-left: 6px;
  font-size: 14px;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.topbar-user {
  font-size: 14px;
  color: #8a7a6a;
}
.topbar-logout {
  background: none;
  border: 1px solid #d4bc9f;
  color: #5c3317;
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.topbar-logout:hover {
  background: #5c3317;
  color: #fff;
}

/* ========================================
   主体
   ======================================== */
.admin-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

/* ========================================
   侧边栏
   ======================================== */
.admin-sidebar {
  width: 200px;
  background: #fff;
  border-right: 1px solid #ede6dc;
  padding: 16px 0;
  flex-shrink: 0;
}
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 8px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border-radius: 8px;
  text-decoration: none;
  color: #5c4a3a;
  font-size: 15px;
  transition: all 0.15s ease;
}
.nav-item:hover {
  background: #f8f4ed;
  color: #3d2b1a;
}
.nav-item.active {
  background: #f4ece0;
  color: #5c3317;
  font-weight: 500;
}
.nav-icon {
  font-size: 18px;
  width: 26px;
  text-align: center;
  flex-shrink: 0;
  color: #b0957a;
  transition: color 0.15s;
}
.nav-item:hover .nav-icon,
.nav-item.active .nav-icon {
  color: #5c3317;
}
.nav-label { white-space: nowrap; }
.nav-arrow { margin-left: auto; font-size: 12px; color: #b0957a; }
.nav-submenu-toggle { cursor: pointer; }
.nav-submenu-items { padding-left: 12px; }
.nav-sub-item {
  display: block; padding: 8px 16px 8px 38px; color: #6b5a4a;
  text-decoration: none; font-size: 14px; border-radius: 8px;
  transition: all 0.15s;
}
.nav-sub-item:hover { background: #f4ece0; color: #5c3317; }
.nav-sub-item.active { background: #f4ece0; color: #5c3317; font-weight: 500; }

/* ========================================
   内容区
   ======================================== */
.admin-content {
  flex: 1;
  min-width: 0;
  padding: 20px 24px;
  overflow-y: auto;
}
</style>
