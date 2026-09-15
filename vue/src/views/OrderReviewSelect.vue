<template>
  <div class="full-wrapper">
    <div class="container">
      <h2 class="section-title">选择评价商品</h2>

      <!-- 全部已评价提示 -->
      <div v-if="allReviewed" class="all-reviewed-tip">
        <p>🎉 该订单所有商品已完成评价</p>
        <button class="btn btn-primary" @click="$router.push('/orders')">返回订单列表</button>
      </div>

      <!-- 商品选择列表 -->
      <div v-else class="review-select-panel">
        <div class="panel-header">
          <span>订单号：{{ orderNo }}</span>
        </div>
        <div class="item-list">
          <div
            v-for="item in items"
            :key="item.id"
            class="as-check-row"
            :class="{ selected: selectedId === item.id }"
            @click="selectItem(item)"
          >
            <span class="as-radio" :class="{ checked: selectedId === item.id }">
              {{ selectedId === item.id ? '●' : '○' }}
            </span>
            <img
              v-if="item.productImage"
              :src="getImg(item.productImage)"
              class="as-check-img"
            />
            <div class="as-check-info">
              <p class="as-check-name">{{ item.productName }}</p>
              <p style="color:#999288;font-size:13px;">
                ¥{{ item.productPrice }} × {{ item.quantity }}
              </p>
            </div>
          </div>
        </div>
        <div class="panel-actions">
          <button class="review-btn review-btn-cancel" @click="$router.push('/orders')">返回</button>
          <button
            class="review-btn review-btn-submit"
            :disabled="!selectedId"
            @click="goReview"
          >去评价</button>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" style="text-align:center;padding:40px;color:#999;">加载中...</div>
    </div>
  </div>
</template>

<script>
import { getUnreviewedItems } from '@/api/order'

export default {
  name: 'OrderReviewSelect',
  data() {
    return {
      orderNo: '',
      items: [],
      allReviewed: false,
      selectedId: null,
      loading: true
    }
  },
  mounted() {
    this.loadItems()
  },
  methods: {
    getImg(path) {
      return this.$resolveImageUrl(path)
    },
    async loadItems() {
      const orderId = this.$route.params.id
      if (!orderId) { this.$router.push('/orders'); return }
      try {
        const res = await getUnreviewedItems(orderId)
        if (res.code === 200 && res.data) {
          this.orderNo = res.data.orderNo || ''
          this.items = res.data.items || []
          this.allReviewed = res.data.allReviewed || false
        }
      } catch (e) {
        console.error(e)
        this.$message.error('加载失败')
      } finally {
        this.loading = false
      }
    },
    selectItem(item) {
      this.selectedId = item.id
    },
    goReview() {
      if (!this.selectedId) return
      const item = this.items.find(i => i.id === this.selectedId)
      if (!item) return
      this.$router.push({
        name: 'OrderReviewWrite',
        params: { id: this.$route.params.id },
        query: {
          itemId: item.id,
          productId: item.productId,
          productName: item.productName,
          productImage: item.productImage
        }
      })
    }
  }
}
</script>

<style scoped>
.full-wrapper { width: 100%; min-height: calc(100vh - 60px); }
.section-title { text-align: center; margin: 24px 0; color: #4a3322; }
.container { max-width: 720px; margin: 0 auto; padding: 0 20px; }

.all-reviewed-tip { text-align: center; padding: 60px 20px; }
.all-reviewed-tip p { font-size: 18px; color: #4a3322; margin-bottom: 20px; }

.review-select-panel {
  background: #fff; border-radius: 16px;
  box-shadow: 0 2px 8px rgba(92,51,23,0.04), 0 8px 32px rgba(92,51,23,0.06);
  overflow: hidden;
}
.panel-header { padding: 16px 24px; color: #999288; font-size: 14px; border-bottom: 1px solid #f8f3ec; }
.item-list { padding: 0 24px; }
.panel-actions { padding: 16px 24px; border-top: 1px solid #f8f3ec; display: flex; gap: 12px; justify-content: flex-end; }

/* 复刻 after-sale 商品选择行样式 */
.as-check-row {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 0; border-bottom: 1px solid #f8f3ec;
  cursor: pointer; transition: background .15s;
}
.as-check-row:hover { background: #fdfaf7; }
.as-check-row.selected { background: #faf5ee; }
.as-radio {
  width: 22px; height: 22px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #b8a090; flex-shrink: 0;
}
.as-radio.checked { color: #966c47; }
.as-check-img { width: 56px; height: 56px; border-radius: 8px; object-fit: cover; flex-shrink: 0; }
.as-check-info { flex: 1; min-width: 0; }
.as-check-name { color: #4a3322; font-size: 15px; font-weight: 500; margin: 0 0 4px; }

/* 复刻暖棕按钮 */
.review-btn {
  padding: 10px 24px; border-radius: 20px; font-size: 15px;
  font-weight: 500; cursor: pointer; border: none; font-family: inherit;
  transition: all .2s;
}
.review-btn-cancel { background: #fff; color: #4a3322; border: 2px solid #966c47; }
.review-btn-cancel:hover { background: #fdfaf5; }
.review-btn-submit { background: #966c47; color: #fff; box-shadow: 0 2px 8px rgba(150,108,71,0.3); }
.review-btn-submit:hover { background: #7d5a3a; }
.review-btn-submit:disabled { opacity: .5; cursor: not-allowed; }
</style>
