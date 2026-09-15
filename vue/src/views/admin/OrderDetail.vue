<template>
  <!-- 订单详情页面 - 1:1复刻 admin/order_detail.jsp -->
  <div>
    <div class="admin-header">
      <h1 class="admin-title">订单详情</h1>
    </div>

    <!-- 订单信息卡片 -->
    <div class="admin-card">
      <div class="admin-card-header">
        <h3 class="admin-card-title">订单信息</h3>
      </div>
      <div class="admin-card-body">
        <div class="admin-form-row">
          <div class="admin-form-group">
            <label class="admin-form-label">订单号</label>
            <div class="admin-form-control">{{ order.orderNo }}</div>
          </div>
          <div class="admin-form-group">
            <label class="admin-form-label">用户ID</label>
            <div class="admin-form-control">{{ order.userId }}</div>
          </div>
          <div class="admin-form-group">
            <label class="admin-form-label">下单时间</label>
            <div class="admin-form-control">{{ order.createTime | formatTime }}</div>
          </div>
          <div class="admin-form-group">
            <label class="admin-form-label">订单状态</label>
            <div class="admin-form-control">{{ getStatusText(order.status) }}</div>
          </div>
        </div>

        <div class="admin-form-row" style="margin-top: 20px;">
          <div class="admin-form-group">
            <label class="admin-form-label">收货人</label>
            <div class="admin-form-control">{{ order.username }}</div>
          </div>
          <div class="admin-form-group">
            <label class="admin-form-label">联系电话</label>
            <div class="admin-form-control">{{ order.phone }}</div>
          </div>
          <div class="admin-form-group" style="grid-column: 1 / -1;">
            <label class="admin-form-label">收货地址</label>
            <div class="admin-form-control">{{ order.address }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 商品清单表格 -->
    <div class="admin-card">
      <div class="admin-card-header">
        <h3 class="admin-card-title">商品列表</h3>
      </div>
      <div class="admin-card-body">
        <div class="admin-table-container">
          <table class="admin-table">
            <thead>
              <tr>
                <th>商品名称</th>
                <th>数量</th>
                <th>单价</th>
                <th>小计</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in order.orderItems" :key="item.id">
                <td>{{ item.productName }}</td>
                <td>{{ item.quantity }}</td>
                <td>¥{{ item.productPrice }}</td>
                <td>¥{{ item.totalPrice }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 订单总价 -->
    <div class="admin-card">
      <div class="admin-card-header">
        <h3 class="admin-card-title">订单总价</h3>
      </div>
      <div class="admin-card-body">
        <div class="admin-stat-card" style="justify-content:center;align-items:center;text-align:center;">
          <div style="font-size:2.5rem;font-weight:bold;color:var(--secondary-color);">
            ¥{{ order.totalPrice }}
          </div>
          <div style="font-size:1.1rem;color:var(--text-light);margin-top:10px;">
            实付款金额
          </div>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="admin-card">
      <div class="admin-card-header">
        <h3 class="admin-card-title">订单操作</h3>
      </div>
      <div class="admin-card-body">
        <div class="admin-form-actions">
          <router-link to="/admin/orders" class="btn btn-secondary">返回订单列表</router-link>
          <div style="display:flex;gap:12px;">
            <a
              v-if="order.status === 1"
              href="javascript:void(0)"
              class="btn btn-success"
              @click="handleUpdateStatus(2)"
            >发货</a>
            <a
              v-if="order.status === 2"
              href="javascript:void(0)"
              class="btn btn-success"
              @click="handleUpdateStatus(3)"
            >完成订单</a>
            <a
              v-if="order.status === 0"
              href="javascript:void(0)"
              class="btn btn-warning"
              @click="handleUpdateStatus(4)"
            >取消订单</a>
            <a
              href="javascript:void(0)"
              class="btn btn-danger"
              @click="handleDelete"
            >删除订单</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/api/request'

export default {
  name: 'AdminOrderDetail',

  data() {
    return {
      order: {}
    }
  },

  mounted() {
    this.loadOrder()
  },

  methods: {
    getStatusText(status) {
      const map = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '售后处理中', 6: '售后已完结' }
      return map[status] || '未知状态'
    },

    async loadOrder() {
      const id = this.$route.params.id
      if (!id) return
      try {
        const res = await request.get(`/admin/orders/${id}`)
        if (res.code === 200 && res.data) {
          this.order = res.data
        }
      } catch (e) {
        console.error('加载订单详情失败:', e)
      }
    },

    async handleUpdateStatus(status) {
      try {
        await request.patch(`/admin/orders/${this.order.id}/status`, { status })
        this.$message.success('操作成功')
        this.loadOrder()
      } catch (e) {
        console.error('操作失败:', e)
      }
    },

    async handleDelete() {
      try {
        await this.$confirm(`确定要删除订单「${this.order.orderNo}」吗？`, '确认删除', {
          confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
        })
        await request.delete(`/admin/orders/${this.order.id}`)
        this.$message.success('删除成功')
        this.$router.push('/admin/orders')
      } catch { /* 用户取消 */ }
    }
  }
}
</script>
