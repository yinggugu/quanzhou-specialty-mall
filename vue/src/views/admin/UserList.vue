<template>
  <!-- 用户管理页面 - 1:1复刻 admin/user_list.jsp -->
  <div>
    <div class="admin-header">
      <h1 class="admin-title">用户管理</h1>
    </div>

    <!-- 搜索过滤 -->
    <div class="admin-search-filter">
      <form @submit.prevent="handleSearch">
        <input
          type="text"
          v-model="keyword"
          class="form-control"
          placeholder="搜索用户名或邮箱..."
        />
        <button type="submit" class="btn btn-primary">搜索</button>
      </form>
    </div>

    <!-- 用户表格 -->
    <div class="admin-table-container">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>邮箱</th>
            <th>手机号</th>
            <th>角色</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.email }}</td>
            <td>{{ user.phone }}</td>
            <td>
              <span v-if="user.role === 1">管理员</span>
              <span v-else>普通用户</span>
            </td>
            <td>
              <span v-if="user.status === 1">正常</span>
              <span v-else>禁用</span>
            </td>
            <td>{{ user.createTime | formatTime }}</td>
            <td>
              <div class="table-actions">
                <router-link
                  :to="`/admin/users/${user.id}/edit`"
                  class="btn btn-primary btn-sm"
                >编辑</router-link>
                <a
                  v-if="user.status === 1"
                  href="javascript:void(0)"
                  class="btn btn-secondary btn-sm"
                  @click="handleUpdateStatus(user.id, 0)"
                >禁用</a>
                <a
                  v-if="user.status === 0"
                  href="javascript:void(0)"
                  class="btn btn-success btn-sm"
                  @click="handleUpdateStatus(user.id, 1)"
                >启用</a>
                <a
                  href="javascript:void(0)"
                  class="btn btn-danger btn-sm"
                  @click="handleDelete(user)"
                >删除</a>
              </div>
            </td>
          </tr>
          <tr v-if="users.length === 0 && !loading">
            <td colspan="8" style="text-align:center;padding:40px;">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination" style="text-align:center;margin-top:30px;">
      <a v-if="currentPage > 1" href="javascript:void(0)" class="btn btn-secondary" @click="changePage(currentPage - 1)">上一页</a>
      <span style="margin:0 15px;line-height:36px;">第 {{ currentPage }} / {{ totalPages }} 页</span>
      <a v-if="currentPage < totalPages" href="javascript:void(0)" class="btn btn-secondary" @click="changePage(currentPage + 1)">下一页</a>
    </div>
  </div>
</template>

<script>
import request from '@/api/request'

export default {
  name: 'AdminUserList',

  data() {
    return {
      users: [],
      currentPage: 1,
      totalPages: 1,
      keyword: '',
      loading: false
    }
  },

  mounted() {
    this.loadUsers()
  },

  methods: {
    async loadUsers() {
      this.loading = true
      try {
        const params = { page: this.currentPage, pageSize: 10 }
        if (this.keyword) params.keyword = this.keyword

        const res = await request.get('/admin/users', { params })
        if (res.code === 200 && res.data) {
          this.users = res.data.list || res.data.records || []
          this.totalPages = res.data.totalPages || res.data.pages || 1
          this.currentPage = res.data.currentPage || res.data.current || 1
        }
      } catch (e) {
        console.error('加载用户列表失败:', e)
      } finally {
        this.loading = false
      }
    },

    handleSearch() {
      this.currentPage = 1
      this.loadUsers()
    },

    changePage(page) {
      this.currentPage = page
      this.loadUsers()
    },

    async handleUpdateStatus(userId, status) {
      try {
        await request.patch(`/admin/users/${userId}/status`, { status })
        this.$message.success(status === 1 ? '已启用' : '已禁用')
        this.loadUsers()
      } catch (e) { console.error('操作失败:', e) }
    },

    async handleDelete(user) {
      try {
        await this.$confirm(`确定要删除用户「${user.username}」吗？`, '确认删除', {
          confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
        })
        await request.delete(`/admin/users/${user.id}`)
        this.$message.success('删除成功')
        this.loadUsers()
      } catch { /* 用户取消 */ }
    }
  }
}
</script>
