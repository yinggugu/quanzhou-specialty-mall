<template>
  <!-- 订单详情页 - 1:1复刻 order/detail.jsp 结构 -->
  <div class="container">
    <h2 class="section-title">订单详情</h2>

    <div v-if="order.id" class="order-detail">
      <!-- 订单基本信息 -->
      <div class="order-detail-info">
        <div class="order-header">
          <h3>订单信息</h3>
          <span class="order-status-badge" :class="getStatusClass(order.afterSaleId ? (order.afterSaleStatus === 4 ? 6 : 5) : order.status)">
            {{ order.afterSaleId ? (order.afterSaleStatus === 4 ? '售后已完结' : '售后中') : getStatusText(order.status) }}
          </span>
        </div>
        <div class="order-info">
          <div class="order-info-item">
            <label>订单编号</label>
            <span>{{ order.orderNo }}</span>
          </div>
          <div class="order-info-item">
            <label>下单时间</label>
            <span>{{ order.createTime }}</span>
          </div>
          <div class="order-info-item">
            <label>收货人</label>
            <span>{{ order.username }}</span>
          </div>
          <div class="order-info-item">
            <label>联系电话</label>
            <span>{{ order.phone }}</span>
          </div>
          <div class="order-info-item">
            <label>收货地址</label>
            <span>{{ order.address }}</span>
          </div>
        </div>
      </div>

      <!-- 订单商品清单 -->
      <div class="order-detail-products">
        <h3>商品清单</h3>
        <div
          v-for="item in order.orderItems"
          :key="item.id"
          class="order-detail-product"
        >
          <img
            v-if="getProductImage(item.productImage)"
            :src="getProductImage(item.productImage)"
            :alt="item.productName"
            class="order-detail-product-image"
          />
          <div class="order-detail-product-info">
            <h4>{{ item.productName }}</h4>
            <p>数量：{{ item.quantity }}</p>
            <p>单价：¥{{ item.productPrice }}</p>
            <p>小计：¥{{ item.totalPrice }}</p>
          </div>
        </div>
      </div>

      <!-- 订单总额 -->
      <div class="order-detail-total">
        <p>订单总额：¥{{ order.totalPrice }}</p>
      </div>

      <!-- 操作按钮 -->
      <div class="order-detail-actions">
        <router-link to="/orders" class="btn btn-secondary">返回列表</router-link>
        <a
          v-if="order.status === 0"
          href="javascript:void(0)"
          class="btn btn-primary"
          @click="handlePay"
        >立即付款</a>
        <a
          v-if="order.status === 0"
          href="javascript:void(0)"
          class="btn btn-danger"
          @click="handleCancel"
        >取消订单</a>
        <a
          v-if="order.status === 2"
          href="javascript:void(0)"
          class="btn btn-success"
          @click="handleConfirmReceipt"
        >确认收货</a>
        <a
          v-if="order.status === 3"
          href="javascript:void(0)"
          class="btn btn-primary"
          @click="$router.push('/orders/'+order.id+'/review')"
        >去评价</a>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading" class="text-center mt-40">
      <div class="loading"></div>
      <p>加载中...</p>
    </div>

    <!-- 订单不存在 -->
    <div v-else class="cart-empty">
      <div class="empty-icon">🔍</div>
      <p>订单不存在</p>
      <router-link to="/orders" class="btn btn-primary">返回订单列表</router-link>
    </div>
    <!-- ✅ 通用确认弹窗 -->
    <div v-if="confirmVisible" class="review-overlay"><div class="review-dialog">
      <div class="review-dialog-header"><h3 class="review-dialog-title">{{confirmTitle}}</h3><span class="review-close" @click="confirmVisible=false">✕</span></div>
      <p class="as-dialog-text">{{confirmMsg}}</p>
      <div class="review-buttons"><button class="review-btn review-btn-cancel" @click="confirmVisible=false">取消</button><button class="review-btn review-btn-submit" @click="doConfirm">{{confirmBtn}}</button></div>
    </div></div>
  </div>
</template>

<script>
import { getOrderDetail, payOrder, cancelOrder, confirmReceipt } from '@/api/order'

export default {
  name: 'OrderDetail',

  data() {
    return {
      order: {},
      loading: false,
      confirmVisible: false, confirmTitle: '', confirmMsg: '', confirmBtn: '', confirmAction: null
    }
  },

  mounted() {
    this.loadOrder()
  },

  methods: {
    getProductImage(imagePath) {
      return this.$resolveImageUrl(imagePath)
    },

    getStatusText(status) {
      const map = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '售后处理中', 6: '售后已完结' }
      return map[status] || '未知状态'
    },

    getStatusClass(status) {
      const map = {
        0: 'status-pending', 1: 'status-shipping', 2: 'status-delivering',
        3: 'status-completed', 4: 'status-cancelled'
      }
      return map[status] || 'status-unknown'
    },

    async loadOrder() {
      const id = this.$route.params.id
      if (!id) {
        this.$router.push('/orders')
        return
      }

      this.loading = true
      try {
        const res = await getOrderDetail(id)
        if (res.code === 200 && res.data) {
          this.order = res.data
        }
      } catch (e) {
        console.error('加载订单详情失败:', e)
      } finally {
        this.loading = false
      }
    },

    handlePay() {
      this.confirmTitle = '确认支付'; this.confirmMsg = '确认支付此订单？'; this.confirmBtn = '确认支付'
      this.confirmAction = async () => { await payOrder(this.order.id); this.$message.success('支付成功'); this.loadOrder() }
      this.confirmVisible = true
    },
    handleCancel() {
      this.confirmTitle = '确认取消'; this.confirmMsg = '确定要取消此订单吗？'; this.confirmBtn = '确认取消'
      this.confirmAction = async () => { await cancelOrder(this.order.id); this.$message.success('订单已取消'); this.loadOrder() }
      this.confirmVisible = true
    },
    handleConfirmReceipt() {
      this.confirmTitle = '确认收货'; this.confirmMsg = '确认已收到商品？'; this.confirmBtn = '确认收货'
      this.confirmAction = async () => { await confirmReceipt(this.order.id); this.$message.success('已确认收货'); this.loadOrder() }
      this.confirmVisible = true
    },
    async doConfirm() { if (this.confirmAction) { await this.confirmAction(); this.confirmVisible = false } },
  }
}
</script>

<style scoped>
.review-overlay{position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.3);z-index:2000;display:flex;align-items:center;justify-content:center;}
.review-dialog{width:460px;max-width:92vw;background:#fdfaf5;border-radius:16px;box-shadow:0 2px 8px rgba(92,51,23,0.04),0 8px 32px rgba(92,51,23,0.08);padding:28px 30px 24px;box-sizing:border-box;font-family:inherit;}
.review-dialog-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;}
.review-dialog-title{margin:0;font-size:1.1rem;font-weight:600;color:#4a3322;}
.review-close{font-size:18px;color:#b8a899;cursor:pointer;line-height:1;}
.review-close:hover{color:#966c47;}
.as-dialog-text{color:#4a3322;font-size:16px;line-height:1.7;margin-bottom:20px;}
.review-buttons{display:flex;gap:12px;justify-content:flex-end;}
.review-btn{padding:10px 24px;border-radius:20px;font-size:15px;font-weight:500;cursor:pointer;border:none;font-family:inherit;}
.review-btn-cancel{background:#fff;color:#4a3322;border:2px solid #966c47;}
.review-btn-submit{background:#966c47;color:#fff;}
.review-btn-submit:hover{background:#7d5a3a;}
</style>
