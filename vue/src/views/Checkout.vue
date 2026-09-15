<template>
  <div class="full-wrapper">
    <h2 class="section-title">订单结算</h2>
    <!-- 订单结算页 - 1:1复刻 order/checkout.jsp 结构 -->
    <div class="container">
      

      <!-- 错误提示 -->
      <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

      <div class="checkout">
        <div class="checkout-left">
          <!-- 收货信息表单 -->
          <h3>📋 收货信息</h3>
          <div class="form-group">
            <label for="username">收货人姓名</label>
            <input
              type="text"
              id="username"
              v-model="form.username"
              class="form-control"
              placeholder="请输入收货人姓名"
            />
          </div>
          <div class="form-group">
            <label for="phone">联系电话</label>
            <input
              type="tel"
              id="phone"
              v-model="form.phone"
              class="form-control"
              required
              placeholder="请输入联系电话"
            />
          </div>
          <div class="form-group">
            <label for="address">收货地址</label>
            <textarea
              id="address"
              v-model="form.address"
              class="form-control"
              rows="3"
              required
              placeholder="请输入收货地址"
            ></textarea>
          </div>

          <!-- 商品清单 -->
          <h3>🛒 商品清单</h3>
          <div class="checkout-items">
            <div
              v-for="item in cartItems"
              :key="item.productId"
              class="checkout-item"
            >
              <img
                v-if="getProductImage(item.productImage)"
                :src="getProductImage(item.productImage)"
                :alt="item.productName"
                class="checkout-item-image"
              />
              <div class="checkout-item-info">
                <h4>{{ item.productName }}</h4>
                <p>数量：{{ item.quantity }}</p>
                <p>单价：¥{{ item.productPrice }}</p>
                <p>
                  小计：¥{{ (item.productPrice * item.quantity).toFixed(2) }}
                </p>
              </div>
            </div>
          </div>

          <!-- 汇总和操作 -->
          <div class="checkout-summary">
            <p>
              商品总数：<strong>{{ totalCount }}</strong>
            </p>
            <p>
              订单总额：<strong>¥{{ totalPrice.toFixed(2) }}</strong>
            </p>
          </div>

          <div class="checkout-actions">
            <button
              class="btn btn-primary btn-large"
              @click="handleSubmit"
              :disabled="submitting"
            >
              {{ submitting ? "提交中..." : "提交订单" }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getCheckoutInfo } from "@/api/cart";
import { createOrder } from "@/api/order";
import { mapGetters } from "vuex";

export default {
  name: "Checkout",

  data() {
    return {
      cartItems: [],
      totalCount: 0,
      totalPrice: 0,
      form: {
        username: "",
        phone: "",
        address: "",
      },
      errorMessage: "",
      submitting: false,
    };
  },

  computed: {
    ...mapGetters(["username"]),
  },

  mounted() {
    this.loadCheckoutInfo();
    // 预填用户信息
    const user = this.$store.state.user;
    if (user) {
      this.form.username = user.username || "";
      this.form.phone = user.phone || "";
      this.form.address = user.address || "";
    }
  },

  methods: {
    getProductImage(imagePath) {
      return this.$resolveImageUrl(imagePath)
    },

    async loadCheckoutInfo() {
      try {
        const res = await getCheckoutInfo();
        if (res.code === 200 && res.data) {
          this.cartItems = res.data.items || [];
          this.totalCount = res.data.totalCount || 0;
          this.totalPrice = res.data.totalPrice || 0;
        }
      } catch (e) {
        console.error("加载结算信息失败:", e);
      }
    },

    validateForm() {
      if (!this.form.username.trim()) {
        this.errorMessage = "请输入收货人姓名";
        return false;
      }
      if (!this.form.phone.trim()) {
        this.errorMessage = "请输入联系电话";
        return false;
      }
      const phoneRegex = /^1[3-9]\d{9}$/;
      if (!phoneRegex.test(this.form.phone.trim())) {
        this.errorMessage = "请输入有效的手机号码";
        return false;
      }
      if (!this.form.address.trim()) {
        this.errorMessage = "请输入收货地址";
        return false;
      }
      return true;
    },

    async handleSubmit() {
      this.errorMessage = "";

      if (!this.validateForm()) return;

      if (this.cartItems.length === 0) {
        this.errorMessage = "购物车为空，无法提交订单";
        return;
      }

      this.submitting = true;
      try {
        const res = await createOrder({
          username: this.form.username.trim(),
          phone: this.form.phone.trim(),
          address: this.form.address.trim(),
        });

        if (res.code === 200) {
          const orderId = res.data?.id || res.data?.orderId;
          this.$message.success("订单提交成功");
          this.$store.dispatch("fetchCartCount");
          if (orderId) {
            this.$router.push(`/orders/${orderId}`);
          } else {
            this.$router.push("/orders");
          }
        }
      } catch (e) {
        this.errorMessage = e.message || "提交订单失败，请重试";
      } finally {
        this.submitting = false;
      }
    },
  },
};
</script>
<style scoped>
/* 新增全屏外层样式 */
.full-wrapper {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.section-title {
  text-align: center;
  width: 100%;
  margin-bottom: 20px;
}
.container {
  max-width: 1020px;
  margin-left: auto;
  margin-right: 30px;
  padding: 0 30px;
}
.checkout-left {
  text-align: left;
}
</style>
