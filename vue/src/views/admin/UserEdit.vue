<template>
  <!-- 用户编辑页面 - 1:1复刻 admin/user_edit.jsp -->
  <div>
    <div class="admin-header">
      <h1 class="admin-title">编辑用户</h1>
      <router-link to="/admin/users" class="btn btn-secondary">返回用户列表</router-link>
    </div>

    <div class="card" style="margin-top: 20px;">
      <div class="card-header">
        <h2>用户信息</h2>
      </div>
      <div class="card-body">
        <!-- 错误提示 -->
        <div v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>

        <form @submit.prevent="handleSubmit">
          <!-- 用户名 -->
          <div class="form-group">
            <label for="username">用户名</label>
            <input
              type="text"
              id="username"
              v-model="form.username"
              class="form-control"
              required
            />
          </div>

          <!-- 密码（可选） -->
          <div class="form-group">
            <label for="password">密码（不修改请留空）</label>
            <input
              type="password"
              id="password"
              v-model="form.password"
              class="form-control"
            />
          </div>

          <!-- 邮箱 -->
          <div class="form-group">
            <label for="email">邮箱</label>
            <input
              type="email"
              id="email"
              v-model="form.email"
              class="form-control"
              required
            />
          </div>

          <!-- 手机号 -->
          <div class="form-group">
            <label for="phone">手机号</label>
            <input
              type="tel"
              id="phone"
              v-model="form.phone"
              class="form-control"
              required
            />
          </div>

          <!-- 地址 -->
          <div class="form-group">
            <label for="address">地址</label>
            <textarea
              id="address"
              v-model="form.address"
              class="form-control"
              rows="3"
            ></textarea>
          </div>

          <!-- 角色 -->
          <div class="form-group">
            <label for="role">角色</label>
            <select id="role" v-model.number="form.role" class="form-control" required>
              <option :value="0">普通用户</option>
              <option :value="1">管理员</option>
            </select>
          </div>

          <!-- 状态 -->
          <div class="form-group">
            <label for="status">状态</label>
            <select id="status" v-model.number="form.status" class="form-control" required>
              <option :value="1">启用</option>
              <option :value="0">禁用</option>
            </select>
          </div>

          <div class="form-actions" style="margin-top: 30px;">
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              {{ submitting ? '保存中...' : '保存修改' }}
            </button>
            <router-link to="/admin/users" class="btn btn-secondary">取消</router-link>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/api/request'

export default {
  name: 'AdminUserEdit',

  data() {
    return {
      form: {
        username: '',
        password: '',
        email: '',
        phone: '',
        address: '',
        role: 0,
        status: 1
      },
      errorMessage: '',
      submitting: false
    }
  },

  mounted() {
    this.loadUser()
  },

  methods: {
    async loadUser() {
      const id = this.$route.params.id
      if (!id) return
      try {
        const res = await request.get(`/admin/users/${id}`)
        if (res.code === 200 && res.data) {
          const u = res.data
          this.form = {
            username: u.username || '',
            password: '',      // 密码不回填
            email: u.email || '',
            phone: u.phone || '',
            address: u.address || '',
            role: u.role != null ? u.role : 0,
            status: u.status != null ? u.status : 1
          }
        }
      } catch (e) {
        console.error('加载用户信息失败:', e)
      }
    },

    async handleSubmit() {
      this.errorMessage = ''

      if (!this.form.username.trim()) {
        this.errorMessage = '请输入用户名'
        return
      }

      this.submitting = true
      try {
        const data = { ...this.form }
        // 密码为空时不传
        if (!data.password) delete data.password

        const res = await request.put(`/admin/users/${this.$route.params.id}`, data)
        if (res.code === 200) {
          this.$message.success('保存成功')
          this.$router.push('/admin/users')
        }
      } catch (e) {
        this.errorMessage = e.message || '保存失败'
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>
