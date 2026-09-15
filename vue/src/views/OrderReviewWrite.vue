<template>
  <div class="full-wrapper">
    <div class="container">
      <h2 class="section-title">商品评价</h2>

      <div class="review-panel">
        <!-- 商品信息 -->
        <div class="product-info" v-if="productName">
          <img v-if="getImg(productImage)" :src="getImg(productImage)" class="product-img" />
          <div class="product-text">
            <p class="product-name">{{ productName }}</p>
          </div>
        </div>

        <!-- 星级评分 -->
        <div class="form-group">
          <label class="form-label">评分</label>
          <div class="star-row">
            <span
              v-for="s in 5" :key="s"
              class="star"
              :class="{ active: s <= score }"
              @click="score = s"
            >{{ s <= score ? '★' : '☆' }}</span>
          </div>
        </div>

        <!-- 评价内容 -->
        <div class="form-group">
          <label class="form-label">评价内容</label>
          <textarea
            v-model="content"
            class="review-textarea"
            rows="4"
            maxlength="500"
            :placeholder="'请分享您的使用感受...(' + content.length + '/500)'"
          ></textarea>
        </div>

        <!-- 匿名 -->
        <div class="form-group">
          <label class="anon-label" @click="isAnonymous = !isAnonymous">
            <span class="anon-checkbox" :class="{ checked: isAnonymous }">{{ isAnonymous ? '✓' : '' }}</span>
            匿名评价
          </label>
        </div>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <button class="review-btn review-btn-cancel" @click="goBack">返回</button>
          <button
            class="review-btn review-btn-submit"
            :disabled="submitting || !content.trim()"
            @click="submitReview"
          >{{ submitting ? '提交中...' : '提交评价' }}</button>
        </div>
      </div>

      <!-- 提交成功提示 -->
      <div v-if="submitted" class="success-overlay">
        <div class="success-dialog">
          <p class="success-icon">✅</p>
          <p class="success-text">评价成功！</p>
          <button class="review-btn review-btn-submit" @click="goBack">继续评价其他商品</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { addComment } from '@/api/product'

export default {
  name: 'OrderReviewWrite',
  data() {
    return {
      productId: null,
      itemId: null,
      productName: '',
      productImage: '',
      score: 5,
      content: '',
      isAnonymous: false,
      submitting: false,
      submitted: false,
      orderId: null
    }
  },
  mounted() {
    this.orderId = this.$route.params.id
    this.itemId = this.$route.query.itemId
    this.productId = this.$route.query.productId
    this.productName = this.$route.query.productName || ''
    this.productImage = this.$route.query.productImage || ''
    if (!this.itemId || !this.productId) {
      this.$message.warning('参数异常')
      this.$router.push('/orders')
    }
  },
  methods: {
    getImg(path) {
      return this.$resolveImageUrl(path)
    },
    goBack() {
      this.$router.push({ name: 'OrderReviewSelect', params: { id: this.orderId } })
    },
    async submitReview() {
      if (!this.content.trim()) { this.$message.warning('请输入评价内容'); return }
      this.submitting = true
      try {
        await addComment({
          productId: Number(this.productId),
          orderItemId: Number(this.itemId),
          score: this.score,
          content: this.content.trim(),
          imgList: '',
          isAnonymous: this.isAnonymous ? 1 : 0
        })
        this.submitted = true
      } catch (e) {
        const msg = e.response?.data?.message || '评价失败'
        this.$message.error(msg)
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.full-wrapper { width: 100%; min-height: calc(100vh - 60px); }
.section-title { text-align: center; margin: 24px 0; color: #4a3322; }
.container { max-width: 600px; margin: 0 auto; padding: 0 20px; }

.review-panel {
  background: #fff; border-radius: 16px; padding: 28px 30px;
  box-shadow: 0 2px 8px rgba(92,51,23,0.04), 0 8px 32px rgba(92,51,23,0.06);
}

.product-info { display: flex; align-items: center; gap: 14px; padding-bottom: 20px; margin-bottom: 20px; border-bottom: 1px solid #f8f3ec; }
.product-img { width: 64px; height: 64px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
.product-name { font-size: 16px; font-weight: 600; color: #4a3322; margin: 0; }

.form-group { margin-bottom: 20px; }
.form-label { display: block; font-size: 14px; color: #999288; margin-bottom: 8px; }

.star-row { display: flex; gap: 4px; }
.star { font-size: 32px; color: #ddd; cursor: pointer; transition: color .15s; user-select: none; }
.star.active { color: #f0a500; }
.star:hover { color: #f0a500; }

.review-textarea {
  width: 100%; box-sizing: border-box;
  background: #fffefc; border: 1.5px solid #d9c8b8; border-radius: 10px;
  padding: 12px 16px; font-size: 15px; color: #4a3322;
  outline: none; resize: vertical; font-family: inherit;
}
.review-textarea:focus { border-color: #966c47; }
.review-textarea::placeholder { color: #c4b49e; }

.anon-label { display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 15px; color: #4a3322; user-select: none; }
.anon-checkbox {
  width: 20px; height: 20px; border: 2px solid #d9c8b8; border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; color: #fff; transition: all .15s;
}
.anon-checkbox.checked { background: #966c47; border-color: #966c47; }

.form-actions { display: flex; gap: 12px; justify-content: flex-end; padding-top: 20px; border-top: 1px solid #f8f3ec; }

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

/* 成功弹窗 */
.success-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.3); z-index: 2000; display: flex; align-items: center; justify-content: center; }
.success-dialog { width: 360px; background: #fff; border-radius: 16px; padding: 40px 30px; text-align: center; box-shadow: 0 8px 40px rgba(0,0,0,0.15); }
.success-icon { font-size: 48px; margin: 0 0 12px; }
.success-text { font-size: 18px; color: #4a3322; font-weight: 600; margin-bottom: 24px; }
</style>
