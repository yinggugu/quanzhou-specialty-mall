<template>
  <!-- 订单支付页 - 1:1复刻 order/pay.jsp 结构 -->
  <div class="container">
    <h2 class="section-title">订单支付</h2>

    <div v-if="order.id" class="order-info cart">
      <div class="order-header">
        <h3>订单信息</h3>
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

      <!-- 商品明细 -->
      <div v-for="item in order.orderItems" :key="item.id" class="cart-item">
        <img
          v-if="getProductImage(item.productImage)"
          :src="getProductImage(item.productImage)"
          :alt="item.productName"
          class="cart-item-image"
        />
        <div class="cart-item-info">
          <h3 class="cart-item-name">{{ item.productName }}</h3>
          <p class="cart-item-price">¥{{ item.productPrice }}</p>
          <p>数量：{{ item.quantity }}</p>
        </div>
      </div>

      <!-- 汇总 -->
      <div class="cart-summary">
        <div class="cart-summary-info">
          <p class="cart-summary-total">应付金额：¥{{ order.totalPrice }}</p>
        </div>
      </div>
    </div>

    <!-- 支付确认 -->
    <div class="payment-confirm cart" v-if="order.id && order.status === 0">
      <div class="payment-actions">
        <button
          class="btn btn-primary btn-large"
          @click="handlePay"
          :disabled="paying"
        >
          {{ paying ? '支付中...' : '确认支付' }}
        </button>
        <router-link to="/orders" class="btn btn-secondary">取消支付</router-link>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading" class="text-center mt-40">
      <div class="loading"></div>
    </div>

    <!-- 订单已完成支付 -->
    <div v-else-if="order.id && order.status !== 0" class="cart-empty">
      <p>该订单{{ order.status === 1 ? '已支付' : '无需支付' }}</p>
      <router-link to="/orders" class="btn btn-primary">返回订单列表</router-link>
    </div>
    <!-- 确认支付弹窗 -->
    <div v-if="confirmVisible" class="review-overlay"><div class="review-dialog">
      <div class="review-dialog-header"><h3 class="review-dialog-title">确认支付</h3><span class="review-close" @click="confirmVisible=false">✕</span></div>
      <p class="as-dialog-text">确认支付 ¥{{order.totalPrice}}？</p>
      <div class="review-buttons"><button class="review-btn review-btn-cancel" @click="confirmVisible=false">取消</button><button class="review-btn review-btn-submit" @click="doPay">确认支付</button></div>
    </div></div>
  </div>
</template>

<script>
import { getOrderDetail, payOrder } from '@/api/order'

export default {
  name: 'PayPage',

  data() {
    return {
      order: {},
      loading: false,
      paying: false,
      confirmVisible: false
    }
  },

  mounted() {
    this.loadOrder()
  },

  methods: {
    getProductImage(imagePath) {
      return this.$resolveImageUrl(imagePath)
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
          // 如果已经支付，直接跳转到订单列表
          if (this.order.status !== 0) {
            this.$message.info('该订单无需支付')
          }
        }
      } catch (e) {
        console.error('加载订单失败:', e)
      } finally {
        this.loading = false
      }
    },

    handlePay(){this.confirmVisible=true},
    async doPay(){this.confirmVisible=false;

      this.paying = true
      try {
        await payOrder(this.order.id)
        this.$message.success('支付成功')
        this.$router.push('/orders')
      } catch (e) {
        console.error('支付失败:', e)
      } finally {
        this.paying = false
      }
    }
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
