<template>
  <!-- 订单管理页面 - 1:1复刻 admin/order_list.jsp -->
  <div>
    <div class="admin-header">
      <h1 class="admin-title">订单管理</h1>
    </div>

    <!-- 搜索过滤 -->
    <div class="admin-search-filter">
      <form @submit.prevent="handleSearch">
        <input
          type="text"
          v-model="keyword"
          class="form-control"
          placeholder="搜索订单号..."
        />
        <select v-model.number="statusFilter" class="form-control">
          <option :value="-1">全部状态</option>
          <option :value="0">待付款</option>
          <option :value="1">已付款</option>
          <option :value="2">已发货</option>
          <option :value="3">已完成</option>
          <option :value="4">已取消</option>
        </select>
        <button type="submit" class="btn btn-primary">搜索</button>
      </form>
    </div>

    <!-- 订单表格 -->
    <div class="admin-table-container">
      <table class="table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>用户ID</th>
            <th>商品数量</th>
            <th>订单金额</th>
            <th>订单状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id">
            <td>{{ order.orderNo }}</td>
            <td>{{ order.userId }}</td>
            <td>{{ order.itemCount || (order.orderItems && order.orderItems.length) || 0 }}</td>
            <td>¥{{ order.totalPrice }}</td>
            <td>
              <span class="admin-badge" :class="getStatusBadge(order.status)">{{ getStatusText(order.status) }}</span>
            </td>
            <td>{{ order.createTime | formatTime }}</td>
            <td>
              <div class="table-actions">
                <router-link
                  :to="`/admin/orders/${order.id}`"
                  class="btn btn-primary btn-sm"
                >查看详情</router-link>
                <a
                  v-if="order.status === 1"
                  href="javascript:void(0)"
                  class="btn btn-success btn-sm"
                  @click="handleShip(order.id)"
                >发货</a>
                <a
                  v-if="order.status === 2"
                  href="javascript:void(0)"
                  class="btn btn-success btn-sm"
                  @click="handleComplete(order.id)"
                >完成订单</a>
                <a
                  href="javascript:void(0)"
                  class="btn btn-danger btn-sm"
                  @click="handleDelete(order)"
                >删除</a>
              </div>
            </td>
          </tr>
          <tr v-if="orders.length === 0 && !loading">
            <td colspan="7" style="text-align:center;padding:40px;">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="admin-pagination">
      <a v-if="currentPage > 1" href="javascript:void(0)" class="btn btn-secondary" @click="changePage(currentPage - 1)">上一页</a>
      <span class="admin-pagination-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
      <a v-if="currentPage < totalPages" href="javascript:void(0)" class="btn btn-secondary" @click="changePage(currentPage + 1)">下一页</a>
    </div>
  </div>
</template>

<script>
import request from '@/api/request'

export default {
  name: 'AdminOrderList',

  data() {
    return {
      orders: [],
      currentPage: 1,
      totalPages: 1,
      keyword: '',
      statusFilter: -1,
      loading: false
    }
  },

  mounted() {
    this.loadOrders()
  },

  methods: {
    // 订单状态文本 (与原 admin/order_list.jsp 一致)
    getStatusText(status) {
      const map = { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消', 5: '售后处理中', 6: '售后已完结' }
      return map[status] || '未知状态'
    },
    getStatusBadge(status) {
      const map = { 0: 'admin-badge-warning', 1: 'admin-badge-info', 2: 'admin-badge-primary', 3: 'admin-badge-success', 4: 'admin-badge-danger', 5: 'admin-badge-warning', 6: 'admin-badge-success' }
      return map[status] || ''
    },

    async loadOrders() {
      this.loading = true
      try {
        const params = { page: this.currentPage, pageSize: 10 }
        if (this.keyword) params.keyword = this.keyword
        if (this.statusFilter >= 0) params.status = this.statusFilter

        const res = await request.get('/admin/orders', { params })
        if (res.code === 200 && res.data) {
          this.orders = res.data.list || res.data.records || []
          this.totalPages = res.data.totalPages || res.data.pages || 1
          this.currentPage = res.data.currentPage || res.data.current || 1
        }
      } catch (e) {
        console.error('加载订单列表失败:', e)
      } finally {
        this.loading = false
      }
    },

    handleSearch() {
      this.currentPage = 1
      this.loadOrders()
    },

    changePage(page) {
      this.currentPage = page
      this.loadOrders()
    },

    async handleShip(orderId) {
      try {
        await request.patch(`/admin/orders/${orderId}/status`, { status: 2 })
        this.$message.success('已标记为发货')
        this.loadOrders()
      } catch (e) { console.error('操作失败:', e) }
    },

    async handleComplete(orderId) {
      try {
        await request.patch(`/admin/orders/${orderId}/status`, { status: 3 })
        this.$message.success('订单已完成')
        this.loadOrders()
      } catch (e) { console.error('操作失败:', e) }
    },

    async handleDelete(order) {
      try {
        await this.$confirm(`确定要删除订单「${order.orderNo}」吗？`, '确认删除', {
          confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
        })
        await request.delete(`/admin/orders/${order.id}`)
        this.$message.success('删除成功')
        this.loadOrders()
      } catch { /* 用户取消 */ }
    }
  }
}
</script>
