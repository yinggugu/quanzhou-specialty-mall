<template>
  <!-- 购物车页 - 1:1复刻 cart.jsp 结构 -->
  <div class="container">
    <h2 class="section-title">购物车</h2>

    <!-- 提示消息区域 -->
    <div v-if="message" class="alert alert-success">{{ message }}</div>
    <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>

    <!-- 购物车内容 -->
    <div class="cart">
      <!-- 空购物车 -->
      <div v-if="!loading && cartItems.length === 0" class="cart-empty">
        <div class="empty-icon">🛒</div>
        <p>您的购物车是空的</p>
        <router-link to="/products" class="btn btn-primary">去购物</router-link>
      </div>

      <!-- 购物车商品列表 -->
      <div v-else-if="cartItems.length > 0" class="cart-items">
        <div v-for="item in cartItems" :key="item.productId" class="cart-item">
          <img
            v-if="getProductImage(item.productImage)"
            :src="getProductImage(item.productImage)"
            :alt="item.productName"
            class="cart-item-image"
          />
          <div class="cart-item-info">
            <h3 class="cart-item-name">{{ item.productName }}</h3>
            <p class="cart-item-price">¥{{ item.productPrice }}</p>
            <div class="cart-item-quantity">
              <div class="quantity-selector">
                <button
                  type="button"
                  class="quantity-btn"
                  :disabled="item.quantity <= 1"
                  @click="updateQuantity(item.productId, item.quantity - 1)"
                >-</button>
                <input
                  type="number"
                  class="quantity-input"
                  :value="item.quantity"
                  :id="`quantity-${item.productId}`"
                  min="1"
                  max="100"
                  @change="handleQuantityInput(item.productId, $event)"
                />
                <button
                  type="button"
                  class="quantity-btn"
                  :disabled="item.quantity >= 100"
                  @click="updateQuantity(item.productId, item.quantity + 1)"
                >+</button>
              </div>
            </div>
          </div>
          <div class="cart-item-actions">
            <a
              href="javascript:void(0)"
              class="btn btn-danger"
              @click="handleDeleteItem(item.productId, item.productName)"
            >删除</a>
          </div>
        </div>

        <!-- 购物车汇总 -->
        <div class="cart-summary">
          <div class="cart-summary-info">
            <p>商品总数：<span id="summary-total-count">{{ totalCount }}</span></p>
            <p>总价格：¥<span id="summary-total-price">{{ totalPrice.toFixed(2) }}</span></p>
          </div>
          <div class="cart-summary-actions">
            <a
              href="javascript:void(0)"
              class="btn btn-danger"
              @click="handleClearCart"
            >清空购物车</a>
            <router-link to="/checkout" class="btn btn-primary">去结算</router-link>
          </div>
        </div>
      </div>
    </div>
    <!-- 确认弹窗 -->
    <div v-if="confirmVisible" class="review-overlay"><div class="review-dialog">
      <div class="review-dialog-header"><h3 class="review-dialog-title">{{confirmTitle}}</h3><span class="review-close" @click="confirmVisible=false">✕</span></div>
      <p class="as-dialog-text">{{confirmMsg}}</p>
      <div class="review-buttons"><button class="review-btn review-btn-cancel" @click="confirmVisible=false">取消</button><button class="review-btn review-btn-submit" @click="doConfirm">{{confirmBtn}}</button></div>
    </div></div>
  </div>
</template>

<script>
import { getCart, updateCartItem, deleteCartItem, clearCart } from '@/api/cart'

export default {
  name: 'Cart',

  data() {
    return {
      cartItems: [],
      totalCount: 0,
      totalPrice: 0,
      message: '',
      confirmVisible: false, confirmTitle: '', confirmMsg: '', confirmBtn: '', confirmAction: null,
      errorMessage: '',
      loading: false
    }
  },

  mounted() {
    this.loadCart()
  },

  methods: {
    getProductImage(imagePath) {
      return this.$resolveImageUrl(imagePath)
    },

    async loadCart() {
      this.loading = true
      try {
        const res = await getCart()
        if (res.code === 200 && res.data) {
          this.cartItems = res.data.items || []
          this.totalCount = res.data.totalCount || 0
          this.totalPrice = res.data.totalPrice || 0
          // 更新 Vuex 中的购物车数量
          this.$store.commit('SET_CART_COUNT', this.totalCount)
        }
      } catch (e) {
        console.error('加载购物车失败:', e)
      } finally {
        this.loading = false
      }
    },

    // 更新商品数量
    async updateQuantity(productId, quantity) {
      if (quantity < 1) return

      // 乐观更新本地数据
      const item = this.cartItems.find(i => i.productId === productId)
      if (item) {
        item.quantity = quantity
      }

      try {
        const res = await updateCartItem(productId, quantity)
        if (res.code === 200 && res.data) {
          // 后端返回了更新后的汇总数据
          this.totalCount = res.data.totalCount
          this.totalPrice = res.data.totalPrice
          this.$store.commit('SET_CART_COUNT', this.totalCount)
        } else {
          // 如果后端没有返回汇总数据，则重新加载
          this.loadCart()
        }
      } catch (e) {
        // 恢复并重新加载
        this.loadCart()
      }
    },

    // 处理数量输入框变化
    handleQuantityInput(productId, event) {
      let quantity = parseInt(event.target.value)
      if (isNaN(quantity) || quantity < 1) {
        quantity = 1
        event.target.value = 1
      } else if (quantity > 100) {
        quantity = 100
        event.target.value = 100
      }
      this.updateQuantity(productId, quantity)
    },

    // 删除单个商品
    handleDeleteItem(productId, productName) {
      this.confirmTitle = '确认删除'; this.confirmMsg = `确定要删除「${productName}」吗？`; this.confirmBtn = '确定'
      this.confirmAction = async () => { await deleteCartItem(productId); this.$message.success('已删除'); this.loadCart() }
      this.confirmVisible = true
    },

    // 清空购物车
    handleClearCart() {
      this.confirmTitle = '确认清空'; this.confirmMsg = '确定要清空购物车吗？此操作不可恢复！'; this.confirmBtn = '确定'
      this.confirmAction = async () => { await clearCart(); this.$message.success('购物车已清空'); this.cartItems = []; this.totalCount = 0; this.totalPrice = 0; this.$store.commit('SET_CART_COUNT', 0) }
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
