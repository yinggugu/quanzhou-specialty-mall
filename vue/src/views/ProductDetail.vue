<template>
  <!-- 商品详情页 - 1:1复刻 product/detail.jsp 结构 -->
  <div class="container">
    <div class="detail-header">
      <h2 class="section-title">商品详情</h2>
      <p class="detail-subtitle" v-if="product.name">{{ product.name }}</p>
    </div>

    <div v-if="product.id" class="product-detail">
      <!-- 左侧商品图片 -->
      <div class="product-detail-left">
        <div class="product-image-wrapper">
          <img
            v-if="getProductImage(product.image)"
            :src="getProductImage(product.image)"
            :alt="product.name"
            class="product-detail-image"
          />
          <!-- 状态徽章 -->
          <div class="product-badge">
            <span v-if="product.status === 1" class="badge badge-success">在售</span>
            <span v-else class="badge badge-danger">已下架</span>
          </div>
        </div>
      </div>

      <!-- 右侧商品信息 -->
      <div class="product-detail-right">
        <!-- 商品名称和标签 -->
        <div class="product-info-header">
          <h3 class="product-detail-name">{{ product.name }}</h3>
          <div class="product-meta-tags">
            <span class="meta-tag" v-if="product.categoryName">{{ product.categoryName }}</span>
            <span class="meta-tag">泉州特产</span>
          </div>
        </div>

        <!-- 价格区域 -->
        <div class="product-price-section">
          <div class="product-detail-price">{{ product.price }}</div>
          <div class="product-stock">
            <span class="stock-icon">📦</span>
            库存：{{ product.stock }} 件
          </div>
        </div>

        <!-- 商品描述 -->
        <div v-if="product.description" class="product-detail-description">
          <h4>商品描述</h4>
          <p>{{ product.description }}</p>
        </div>

        <!-- 操作区域 -->
        <div class="product-detail-actions">
          <div v-if="product.status === 1" class="add-to-cart-form">
            <div class="quantity-control">
              <label>购买数量：</label>
              <div class="quantity-selector">
                <button
                  type="button"
                  class="quantity-btn"
                  :disabled="quantity <= 1"
                  @click="decreaseQuantity"
                >-</button>
                <input
                  type="number"
                  class="quantity-input"
                  v-model.number="quantity"
                  :min="1"
                  :max="product.stock"
                  @change="validateQuantity"
                />
                <button
                  type="button"
                  class="quantity-btn"
                  :disabled="quantity >= product.stock"
                  @click="increaseQuantity"
                >+</button>
              </div>
            </div>
            <button
              class="btn btn-primary btn-large"
              @click="handleAddToCart"
              :disabled="addingToCart"
            >
              <span class="btn-icon">🛒</span>
              {{ addingToCart ? '添加中...' : '加入购物车' }}
            </button>
          </div>
          <div v-else class="cart-empty">
            <p>该商品已下架</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading" class="text-center mt-40">
      <div class="loading"></div>
      <p>加载中...</p>
    </div>

    <!-- 商品不存在 -->
    <div v-else class="cart-empty">
      <div class="empty-icon">🔍</div>
      <p>商品不存在或已下架</p>
      <router-link to="/products" class="btn btn-primary">返回商品列表</router-link>
    </div>

    <!-- ✅ 评价区域 -->
    <div v-if="product.id" class="comment-section" style="margin-top:40px;padding:25px 30px;background:#fff;border-radius:12px;box-shadow:0 1px 6px rgba(92,51,23,0.04);">
      <h3 style="color:#3d2b1a;margin-bottom:16px;">商品评价</h3>
      <!-- 统计 -->
      <div style="display:flex;gap:30px;margin-bottom:16px;flex-wrap:wrap;align-items:center;">
        <span style="font-size:22px;color:#c0392b;font-weight:600;">{{ commentStats.avgScore || 0 }} 分</span>
        <span style="color:#666;">共 {{ commentStats.total || 0 }} 条评价</span>
        <span style="color:#666;">好评率 {{ commentStats.goodRate || 0 }}%</span>
      </div>
      <!-- 筛选 -->
      <div style="display:flex;gap:10px;margin-bottom:16px;flex-wrap:wrap;align-items:center;">
        <button :class="['btn', filterType==='all'?'btn-primary':'btn-secondary']" @click="filterType='all';loadComments()">全部评价</button>
        <button :class="['btn', filterType==='image'?'btn-primary':'btn-secondary']" @click="filterType='image';loadComments()">带图评价</button>
        <select v-model="sortType" class="form-control" style="width:140px;margin-left:auto;" @change="loadComments">
          <option value="newest">最新评价</option><option value="good">好评优先</option><option value="bad">差评优先</option>
        </select>
      </div>
      <!-- 列表 -->
      <div v-if="commentList.length===0" class="cart-empty"><div class="empty-icon">💬</div><p>暂无用户评价，快来抢首评吧！</p></div>
      <div v-for="c in commentList" :key="c.id" style="padding:12px 0;border-top:1px solid #f0e8dc;">
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <div style="display:flex;align-items:center;gap:8px;">
            <!-- ✅ 全局文字头像组件 -->
            <UserAvatar :username="c.isAnonymous ? c.nickname : (c.username || c.nickname)" :size="36" />
            <!-- ✅ 实名显示原用户名，匿名显示脱敏昵称 -->
            <span style="font-weight:500;color:#3d2b1a;">{{ c.isAnonymous ? c.nickname : (c.username || c.nickname) }}</span>
          </div>
          <span style="color:#f0ad4e;">{{ '★'.repeat(c.score) }}{{ '☆'.repeat(5-c.score) }}</span>
        </div>
        <div style="color:#666;font-size:13px;margin:4px 0;">{{ c.createTime }}</div>
        <p style="color:#3d2b1a;margin:4px 0;">{{ c.content }}</p>
        <div v-if="c.imgList && c.imgList.length>0" style="display:flex;gap:6px;margin:4px 0;">
          <img v-for="(img,i) in c.imgList" :key="i" :src="img" style="width:60px;height:60px;object-fit:cover;border-radius:4px;" />
        </div>
        <div style="display:flex;gap:12px;align-items:center;">
          <a href="javascript:void(0)" style="color:#b0957a;font-size:12px;" @click="handleLike(c)">👍 {{ c.likeCount || 0 }}</a>
          <span v-if="c.replyContent" style="color:#5c3317;font-size:12px;background:#fdf6ee;padding:4px 10px;border-radius:4px;">客服回复：{{ c.replyContent }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getProductDetail, getCommentList, likeComment } from '@/api/product'
import { addToCart } from '@/api/cart'
import { mapGetters } from 'vuex'
import UserAvatar from '@/components/UserAvatar.vue'

export default {
  name: 'ProductDetail',
  components: { UserAvatar },

  data() {
    return {
      product: {},
      quantity: 1,
      loading: false,
      addingToCart: false,
      // ✅ 评价
      commentList: [],
      commentStats: {},
      filterType: 'all',
      sortType: 'newest'
    }
  },

  computed: {
    ...mapGetters(['isLoggedIn'])
  },

  mounted() {
    this.loadProduct()
  },

  methods: {
    getProductImage(imagePath) {
      return this.$resolveImageUrl(imagePath)
    },

    async loadProduct() {
      const id = this.$route.params.id
      if (!id) {
        this.$router.push('/products')
        return
      }

      this.loading = true
      try {
        const res = await getProductDetail(id)
        if (res.code === 200 && res.data) {
          this.product = res.data
          // ✅ 商品加载完成后自动拉取评价
          this.loadComments()
        }
      } catch (e) {
        console.error('加载商品详情失败:', e)
      } finally {
        this.loading = false
      }
    },

    decreaseQuantity() {
      if (this.quantity > 1) {
        this.quantity--
      }
    },

    increaseQuantity() {
      if (this.quantity < this.product.stock) {
        this.quantity++
      }
    },

    validateQuantity() {
      if (!this.quantity || this.quantity < 1) {
        this.quantity = 1
      } else if (this.quantity > this.product.stock) {
        this.quantity = this.product.stock
        this.$message.warning(`库存不足，最多可购买 ${this.product.stock} 件`)
      }
    },

    async handleAddToCart() {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录后再添加商品到购物车')
        this.$router.push({ name: 'Login', query: { redirect: this.$route.fullPath } })
        return
      }

      if (this.quantity > this.product.stock) {
        this.$message.warning('库存不足')
        return
      }

      this.addingToCart = true
      try {
        await addToCart({
          productId: this.product.id,
          quantity: this.quantity
        })
        this.$message.success(`已成功将「${this.product.name}」加入购物车`)
        this.$store.dispatch('fetchCartCount')
      } catch (e) {
        console.error('加入购物车失败:', e)
      } finally {
        this.addingToCart = false
      }
    },

    // ✅ 加载评价
    async loadComments() {
      if (!this.product.id) return
      try {
        const res = await getCommentList({ productId: this.product.id, filterType: this.filterType, sortType: this.sortType, page: 1, pageSize: 50 })
        if (res.code === 200 && res.data) {
          this.commentList = res.data.list || []
          this.commentStats = { total: res.data.total, avgScore: res.data.avgScore, goodRate: res.data.goodRate }
        }
      } catch (e) { console.error(e) }
    },

    // ✅ 点赞
    async handleLike(comment) {
      try {
        await likeComment(comment.id)
        comment.likeCount = (comment.likeCount || 0) + 1
      } catch (e) { console.error(e) }
    }
  }
}
</script>
