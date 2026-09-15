<template>
  <!-- 管理员仪表盘 - 1:1复刻 admin/index.jsp -->
  <div>
    <div class="admin-header">
      <h1 class="admin-title">仪表盘</h1>
    </div>

    <div class="admin-dashboard">
      <!-- 商品总数 -->
      <div class="admin-stat-card">
        <div class="admin-stat-card-header">
          <h3>商品总数</h3>
          <i class="el-icon-s-goods stat-icon"></i>
        </div>
        <p class="stat-number">{{ stats.totalProducts }}</p>
      </div>

      <!-- 订单总数 -->
      <div class="admin-stat-card">
        <div class="admin-stat-card-header">
          <h3>订单总数</h3>
          <i class="el-icon-s-order stat-icon"></i>
        </div>
        <p class="stat-number">{{ stats.totalOrders }}</p>
      </div>

      <!-- 用户总数 -->
      <div class="admin-stat-card">
        <div class="admin-stat-card-header">
          <h3>用户总数</h3>
          <i class="el-icon-user stat-icon"></i>
        </div>
        <p class="stat-number">{{ stats.totalUsers }}</p>
      </div>

      <!-- 今日订单 -->
      <div class="admin-stat-card">
        <div class="admin-stat-card-header">
          <h3>今日订单</h3>
          <i class="el-icon-s-data stat-icon"></i>
        </div>
        <p class="stat-number">{{ stats.todayOrders }}</p>
      </div>

      <!-- ✅ 评价统计卡片 -->
      <div class="admin-stat-card" @click="$router.push('/admin/comments')" style="cursor:pointer;">
        <div class="admin-stat-card-header"><h3>总评价数</h3><i class="el-icon-chat-line-square stat-icon"></i></div>
        <p class="stat-number">{{ commentStats.total || 0 }}</p>
      </div>
      <div class="admin-stat-card" @click="$router.push('/admin/comments')" style="cursor:pointer;">
        <div class="admin-stat-card-header"><h3>平均好评率</h3><i class="el-icon-star-on stat-icon"></i></div>
        <p class="stat-number">{{ commentStats.avgRate || 0 }}%</p>
      </div>
      <div class="admin-stat-card" @click="$router.push('/admin/comments')" style="cursor:pointer;">
        <div class="admin-stat-card-header"><h3>带图评价</h3><i class="el-icon-picture stat-icon"></i></div>
        <p class="stat-number">{{ commentStats.imageCount || 0 }}</p>
      </div>
      <div class="admin-stat-card" @click="$router.push('/admin/comments')" style="cursor:pointer;">
        <div class="admin-stat-card-header"><h3>待回复评价</h3><i class="el-icon-edit stat-icon"></i></div>
        <p class="stat-number" style="color:#d43c2e;">{{ commentStats.pendingReply || 0 }}</p>
      </div>

      <!-- ✅ 售后统计卡片 -->
      <div class="admin-stat-card" @click="$router.push('/admin/afterSale')" style="cursor:pointer;">
        <div class="admin-stat-card-header"><h3>待审核售后</h3><i class="el-icon-s-claim stat-icon"></i></div>
        <p class="stat-number" style="color:#e67e22;">{{ afterSaleStats.pending || 0 }}</p>
      </div>
      <div class="admin-stat-card" @click="$router.push('/admin/afterSale')" style="cursor:pointer;">
        <div class="admin-stat-card-header"><h3>本月退货量</h3><i class="el-icon-refresh stat-icon"></i></div>
        <p class="stat-number">{{ afterSaleStats.monthTotal || 0 }}</p>
      </div>
      <div class="admin-stat-card" @click="$router.push('/admin/afterSale')" style="cursor:pointer;">
        <div class="admin-stat-card-header"><h3>已完成售后</h3><i class="el-icon-circle-check stat-icon"></i></div>
        <p class="stat-number" style="color:#27ae60;">{{ afterSaleStats.finished || 0 }}</p>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/api/request'

export default {
  name: 'AdminDashboard',

  data() {
    return {
      stats: {
        totalProducts: 0, totalOrders: 0, totalUsers: 0, todayOrders: 0
      },
      commentStats: { total: 0, avgRate: 0, imageCount: 0, pendingReply: 0 },
      afterSaleStats: { pending: 0, monthTotal: 0, finished: 0 }
    }
  },

  mounted() {
    this.loadStats()
    this.loadCommentStats()
    this.loadAfterSaleStats()
  },

  methods: {
    async loadStats() {
      try {
        const res = await request.get('/admin/dashboard')
        if (res.code === 200 && res.data) { this.stats = res.data }
      } catch (e) { console.error('加载仪表盘数据失败:', e) }
    },
    async loadCommentStats() {
      try {
        const res = await request.get('/admin/comment/stats')
        if (res.code === 200 && res.data) { this.commentStats = res.data }
      } catch (e) { console.error(e) }
    },
    async loadAfterSaleStats() {
      try {
        const res = await request.get('/admin/afterSale/stats')
        if (res.code === 200 && res.data) { this.afterSaleStats = res.data }
      } catch (e) { console.error(e) }
    }
  }
}
</script>
