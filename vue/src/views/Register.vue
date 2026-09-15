<template>
  <!-- 注册页 - 1:1复刻 register.jsp 结构 -->
  <div class="container">
    <div class="auth-form-container">
      <h2 class="form-title">用户注册</h2>

      <form class="auth-form" @submit.prevent="handleRegister">
        <!-- 错误提示 -->
        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

        <!-- 用户名 -->
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            type="text"
            id="username"
            v-model="form.username"
            required
            placeholder="请输入用户名（3-20个字符）"
          />
        </div>

        <!-- 密码 -->
        <div class="form-group">
          <label for="password">密码</label>
          <input
            type="password"
            id="password"
            v-model="form.password"
            required
            placeholder="请输入密码（6-20位，需包含字母和数字）"
          />
        </div>

        <!-- 邮箱 -->
        <div class="form-group">
          <label for="email">邮箱</label>
          <input
            type="email"
            id="email"
            v-model="form.email"
            required
            placeholder="请输入邮箱地址"
          />
        </div>

        <!-- 手机号 -->
        <div class="form-group">
          <label for="phone">手机号</label>
          <input
            type="tel"
            id="phone"
            v-model="form.phone"
            required
            placeholder="请输入手机号码"
          />
        </div>

        <!-- 地址 -->
        <div class="form-group">
          <label for="address">地址</label>
          <input
            type="text"
            id="address"
            v-model="form.address"
            required
            placeholder="请输入收货地址"
          />
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="loading">
            {{ loading ? '注册中...' : '注册' }}
          </button>
          <router-link to="/login" class="btn btn-secondary">已有账号？去登录</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { register } from '@/api/auth'

export default {
  name: 'Register',

  data() {
    return {
      form: {
        username: '',
        password: '',
        email: '',
        phone: '',
        address: ''
      },
      errorMessage: '',
      loading: false
    }
  },

  methods: {
    // 验证表单
    validateForm() {
      const { username, password, email, phone, address } = this.form

      // 用户名验证：字母/数字/下划线，长度3-20
      const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/
      if (!usernameRegex.test(username)) {
        this.errorMessage = '用户名只能包含字母、数字和下划线，长度3-20个字符'
        return false
      }

      // 密码验证：长度6-20，需包含字母和数字
      if (password.length < 6 || password.length > 20) {
        this.errorMessage = '密码长度需要6-20个字符'
        return false
      }
      if (!/[a-zA-Z]/.test(password) || !/\d/.test(password)) {
        this.errorMessage = '密码需要包含字母和数字'
        return false
      }

      // 邮箱验证
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(email)) {
        this.errorMessage = '请输入有效的邮箱地址'
        return false
      }

      // 手机号验证
      const phoneRegex = /^1[3-9]\d{9}$/
      if (!phoneRegex.test(phone)) {
        this.errorMessage = '请输入有效的手机号码'
        return false
      }

      // 地址验证
      if (!address.trim() || address.length > 100) {
        this.errorMessage = '请输入有效的收货地址（不超过100个字符）'
        return false
      }

      return true
    },

    async handleRegister() {
      this.errorMessage = ''

      if (!this.validateForm()) {
        return
      }

      this.loading = true

      try {
        await register({
          username: this.form.username,
          password: this.form.password,
          email: this.form.email,
          phone: this.form.phone,
          address: this.form.address
        })

        this.$message.success('注册成功，请登录')
        this.$router.push('/login')
      } catch (e) {
        this.errorMessage = e.message || '注册失败，请重试'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>
