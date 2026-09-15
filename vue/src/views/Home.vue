<template>
  <!-- 首页 - 1:1复刻 index.jsp 结构 -->
  <div>
    <!-- 轮播图区域 -->
    <div id="main-carousel" class="carousel">
      <div class="carousel-inner">
        <div
          v-for="(slide, index) in slides"
          :key="index"
          class="carousel-item"
          :class="{ active: currentSlide === index }"
        >
          <img v-if="slide.image" :src="slide.image" :alt="slide.alt" />
          <div class="carousel-caption">
            <h3>{{ slide.title }}</h3>
            <p>{{ slide.description }}</p>
          </div>
        </div>
      </div>
      <button class="carousel-control prev" @click="prevSlide">&lt;</button>
      <button class="carousel-control next" @click="nextSlide">&gt;</button>
    </div>

    <!-- 主要内容区 -->
    <div class="container">
      <!-- 走进泉州 - 入口卡片区 -->
      <div class="intro-section">
        <div class="intro-header">
          <h2 class="section-title">走进泉州</h2>
          <p class="intro-subtitle">探索千年古城，品味闽南文化</p>
        </div>
        <div class="card-grid intro-grid">
          <!-- 商城购物入口 -->
          <router-link to="/products" class="card intro-card">
            <div class="card-icon-wrapper">
              <div class="card-icon">🛍️</div>
            </div>
            <div class="card-body">
              <h3 class="card-title">商城购物</h3>
              <p class="card-text">正宗泉州特产，精选茶叶、糕点、海鲜等特色商品</p>
              <div class="card-features">
                <span class="feature-tag">🍵 茶叶</span>
                <span class="feature-tag">🥮 糕点</span>
                <span class="feature-tag">🦐 海鲜</span>
              </div>
              <div class="card-actions">
                <span class="btn btn-primary card-btn">进入商城 →</span>
              </div>
            </div>
          </router-link>

          <!-- 文化介绍入口 -->
          <router-link to="/quanzhou" class="card intro-card">
            <div class="card-icon-wrapper">
              <div class="card-icon">🏛️</div>
            </div>
            <div class="card-body">
              <h3 class="card-title">文化介绍</h3>
              <p class="card-text">深入了解泉州历史、美食、工艺等闽南文化精髓</p>
              <div class="card-features">
                <span class="feature-tag">📜 历史</span>
                <span class="feature-tag">🍜 美食</span>
                <span class="feature-tag">🎨 工艺</span>
              </div>
              <div class="card-actions">
                <span class="btn btn-primary card-btn">了解更多 →</span>
              </div>
            </div>
          </router-link>
        </div>
      </div>

      <!-- 特色推荐 - 商品卡片网格 -->
      <div class="section">
        <h2 class="section-title">特色推荐</h2>
        <div class="card-grid">
          <div v-for="product in recommendedProducts" :key="product.id" class="card">
            <img
              v-if="getProductImage(product.image)"
              :src="getProductImage(product.image)"
              :alt="product.name"
              class="card-img"
            />
            <div class="card-body">
              <h3 class="card-title">{{ product.name }}</h3>
              <p class="card-price">{{ product.price }}</p>
              <p class="card-stock">库存：{{ product.stock }}</p>
              <p class="card-status">
                <template v-if="product.status === 1">状态：在售</template>
                <template v-else>状态：已下架</template>
              </p>
              <div class="card-actions">
                <router-link
                  :to="`/products/${product.id}`"
                  class="btn btn-secondary card-btn"
                >查看详情</router-link>
                <a
                  v-if="product.status === 1"
                  href="javascript:void(0)"
                  class="btn btn-primary card-btn add-to-cart-btn"
                  @click="handleAddToCart(product)"
                >加入购物车</a>
              </div>
            </div>
          </div>
        </div>
        <div class="text-center mt-20">
          <router-link to="/products" class="btn btn-primary">查看更多商品</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getRecommendedProducts } from '@/api/product'
import { addToCart } from '@/api/cart'

export default {
  name: 'Home',

  data() {
    return {
      // 轮播图数据（与原JSP完全一致）
      slides: [
        {
          image: this.$resolveImageUrl('quanzhou-1.jpg'),
          alt: '泉州风光',
          title: '泉州 - 海上丝绸之路起点',
          description: '品味闽南文化，选购正宗特产'
        },
        {
          image: this.$resolveImageUrl('quanzhou-2.jpg'),
          alt: '泉州特产',
          title: '泉州特产 - 传统美食',
          description: '尝遍泉州地道美食，感受闽南风味'
        },
        {
          image: this.$resolveImageUrl('quanzhou-3.jpg'),
          alt: '泉州工艺',
          title: '非遗工艺 - 传承千年',
          description: '选购精美手工艺品，传承闽南文化'
        }
      ],
      currentSlide: 0,
      recommendedProducts: []
    }
  },

  mounted() {
    // 初始化轮播图（与原 main.js 中 initCarousel 逻辑一致）
    this.initCarousel()
    // 加载推荐商品
    this.loadRecommendedProducts()
  },

  beforeDestroy() {
    // 清除轮播定时器
    if (this.carouselTimer) {
      clearInterval(this.carouselTimer)
    }
  },

  methods: {
    // 轮播图控制
    initCarousel() {
      this.carouselTimer = setInterval(() => {
        this.nextSlide()
      }, 5000)
    },

    prevSlide() {
      let index = this.currentSlide - 1
      if (index < 0) index = this.slides.length - 1
      this.currentSlide = index
    },

    nextSlide() {
      let index = this.currentSlide + 1
      if (index >= this.slides.length) index = 0
      this.currentSlide = index
    },

    // 处理商品图片路径
    getProductImage(imagePath) {
      return this.$resolveImageUrl(imagePath)
    },

    // 加载推荐商品
    async loadRecommendedProducts() {
      try {
        const res = await getRecommendedProducts(3)
        if (res.code === 200 && res.data) {
          this.recommendedProducts = Array.isArray(res.data)
            ? res.data
            : (res.data.list || [])
        }
      } catch (e) {
        console.error('加载推荐商品失败:', e)
      }
    },

    // 加入购物车
    async handleAddToCart(product) {
      // 检查是否登录
      if (!this.$store.getters.isLoggedIn) {
        this.$message.warning('请先登录后再添加商品到购物车')
        this.$router.push({ name: 'Login', query: { redirect: this.$route.fullPath } })
        return
      }

      try {
        await addToCart({ productId: product.id, quantity: 1 })
        this.$message.success(`已成功将「${product.name}」加入购物车`)
        // 刷新购物车数量
        this.$store.dispatch('fetchCartCount')
      } catch (e) {
        console.error('加入购物车失败:', e)
      }
    }
  }
}
</script>
