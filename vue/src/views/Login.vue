<template>
  <!-- 登录页 - ✅ 双表拆分：根据路由判断登录类型 -->
  <div class="container">
    <div class="auth-form-container">
      <h2 class="form-title">{{ isAdminLogin ? '管理员登录' : '用户登录' }}</h2>

      <form class="auth-form" @submit.prevent="handleLogin">
        <!-- 错误提示 -->
        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

        <div class="form-group">
          <label for="username">用户名</label>
          <input
            type="text"
            id="username"
            v-model="form.username"
            required
            placeholder="请输入用户名"
          />
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
            type="password"
            id="password"
            v-model="form.password"
            required
            placeholder="请输入密码"
          />
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
          <router-link v-if="!isAdminLogin" to="/register" class="btn btn-secondary">注册新用户</router-link>
        </div>

        <!-- ✅ 管理员/用户登录切换入口 -->
        <div class="login-switch">
          <template v-if="!isAdminLogin">
            <span class="switch-text">管理员?</span>
            <a href="/#/login?admin=1" class="switch-link">管理员登录</a>
          </template>
          <template v-else>
            <span class="switch-text">用户?</span>
            <a href="/#/login" class="switch-link">用户登录</a>
          </template>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { parseToken } from '@/api/auth'

export default {
  name: 'Login',

  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      errorMessage: '',
      loading: false
    }
  },

  computed: {
    // ✅ 通过 URL query 参数 ?admin=1 区分管理员登录
    isAdminLogin() {
      return this.$route.query.admin === '1'
    }
  },

  methods: {
    async handleLogin() {
      if (!this.form.username.trim()) {
        this.errorMessage = '请输入用户名'
        return
      }
      if (!this.form.password) {
        this.errorMessage = '请输入密码'
        return
      }

      this.errorMessage = ''
      this.loading = true

      try {
        if (this.isAdminLogin) {
          // ✅ 管理员登录 → 调 adminLogin API
          await this.$store.dispatch('adminLogin', {
            username: this.form.username.trim(),
            password: this.form.password
          })
        } else {
          // ✅ 用户登录 → 调 userLogin API
          await this.$store.dispatch('userLogin', {
            username: this.form.username.trim(),
            password: this.form.password
          })
        }

        this.$message.success('登录成功')

        // ✅ 根据 JWT type 字段判断跳转
        const token = this.$store.state.token
        const info = parseToken(token)
        if (info && info.type === 'admin') {
          this.$router.push('/admin')
        } else {
          const redirect = this.$route.query.redirect || '/'
          this.$router.push(redirect)
        }
      } catch (e) {
        this.errorMessage = e.message || '登录失败，请重试'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-switch {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
}
.switch-text {
  color: #b0957a;
  margin-right: 6px;
}
.switch-link {
  color: #5c3317;
  text-decoration: none;
  font-weight: 500;
  cursor: pointer;
}
.switch-link:hover {
  text-decoration: underline;
}
</style>
