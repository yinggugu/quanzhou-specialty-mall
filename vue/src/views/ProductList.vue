<template>
  <!-- 商品列表页 - 1:1复刻 product/list.jsp 结构 -->
  <div class="container">
    <h2 class="section-title">商品列表</h2>

    <!-- ✅ 商品模糊搜索框 — 与分类栏等宽，无白底，融入页面 -->
    <div class="search-bar-wrapper">
      <input
        type="text"
        v-model="searchGoodsName"
        class="search-input"
        placeholder="请输入商品名称模糊搜索"
        @keydown.enter="handleSearch"
      />
      <button class="btn btn-primary" @click="handleSearch">搜索</button>
      <button class="btn btn-secondary" @click="resetSearch">重置</button>
    </div>

    <!-- 商品分类筛选栏 -->
    <div class="category-filters">
      <h3>商品分类</h3>
      <ul>
        <li>
          <a
            href="javascript:void(0)"
            :class="{ active: !currentCategoryId }"
            @click="filterByCategory(null)"
          >全部商品</a>
        </li>
        <li v-for="cat in categories" :key="cat.id">
          <a
            href="javascript:void(0)"
            :class="{ active: currentCategoryId === cat.id }"
            @click="filterByCategory(cat.id)"
          >{{ cat.name }}</a>
        </li>
      </ul>
    </div>

    <!-- 商品网格列表 -->
    <div class="product-grid">
      <div v-for="product in products" :key="product.id" class="product-card">
        <img
          v-if="getProductImage(product.image)"
          :src="getProductImage(product.image)"
          :alt="product.name"
          class="product-image"
        />
        <div class="product-info">
          <h3 class="product-name">{{ product.name }}</h3>
          <p class="product-price">{{ product.price }}</p>
          <p class="product-stock">库存：{{ product.stock }}</p>
          <p class="product-status">
            <template v-if="product.status === 1">状态：在售</template>
            <template v-else>状态：已下架</template>
          </p>
          <div class="product-actions">
            <router-link
              :to="`/products/${product.id}`"
              class="btn btn-secondary"
            >查看详情</router-link>
            <a
              v-if="product.status === 1"
              href="javascript:void(0)"
              class="btn btn-primary"
              @click="handleAddToCart(product)"
            >加入购物车</a>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="products.length === 0 && !loading" class="cart-empty">
      <div class="empty-icon">📦</div>
      <p>暂无商品</p>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination">
      <a
        v-if="currentPage > 1"
        href="javascript:void(0)"
        class="page-link"
        @click="changePage(currentPage - 1)"
      >上一页</a>
      <span class="page-info">第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
      <a
        v-if="currentPage < totalPages"
        href="javascript:void(0)"
        class="page-link"
        @click="changePage(currentPage + 1)"
      >下一页</a>
    </div>
  </div>
</template>

<script>
import { getProductList, getCategories } from '@/api/product'
import { addToCart } from '@/api/cart'
import { mapGetters } from 'vuex'

export default {
  name: 'ProductList',

  data() {
    return {
      products: [],
      categories: [],
      currentPage: 1,
      totalPages: 1,
      currentCategoryId: null,
      searchGoodsName: '',  // ✅ 模糊搜索关键词
      loading: false
    }
  },

  computed: {
    ...mapGetters(['isLoggedIn'])
  },

  mounted() {
    this.loadCategories()
    this.loadProducts()
  },

  methods: {
    // 处理商品图片路径
    getProductImage(imagePath) {
      return this.$resolveImageUrl(imagePath)
    },

    // 加载分类
    async loadCategories() {
      try {
        const res = await getCategories()
        if (res.code === 200 && res.data) {
          this.categories = Array.isArray(res.data) ? res.data : (res.data.list || [])
        }
      } catch (e) {
        console.error('加载分类失败:', e)
        // 使用默认分类
        this.categories = [
          { id: 1, name: '茶叶' },
          { id: 2, name: '糕点' },
          { id: 3, name: '海鲜' },
          { id: 4, name: '工艺品' }
        ]
      }
    },

    // 加载商品列表
    async loadProducts() {
      this.loading = true
      try {
        const params = {
          page: this.currentPage,
          pageSize: 12
        }
        if (this.currentCategoryId) {
          params.categoryId = this.currentCategoryId
        }
        // ✅ 模糊搜索：去除首尾空格后不为空才传递 goodsName
        const keyword = this.searchGoodsName.trim()
        if (keyword) {
          params.goodsName = keyword
        }

        const res = await getProductList(params)
        if (res.code === 200 && res.data) {
          this.products = res.data.list || res.data.records || []
          this.totalPages = res.data.totalPages || res.data.pages || 1
          this.currentPage = res.data.currentPage || res.data.current || 1
        }
      } catch (e) {
        console.error('加载商品列表失败:', e)
      } finally {
        this.loading = false
      }
    },

    // 按分类筛选
    filterByCategory(categoryId) {
      this.currentCategoryId = categoryId
      this.currentPage = 1
      this.loadProducts()
    },

    // ✅ 搜索：携带关键词刷新
    handleSearch() {你描述一下管理员退款框
      this.currentPage = 1
      this.loadProducts()
    },

    // ✅ 重置搜索：清空关键词 + 恢复全部商品
    resetSearch() {
      this.searchGoodsName = ''
      this.currentPage = 1
      this.loadProducts()
    },

    // 切换页码
    changePage(page) {
      this.currentPage = page
      this.loadProducts()
      // 滚动到页面顶部
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },

    // 加入购物车
    async handleAddToCart(product) {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录后再添加商品到购物车')
        this.$router.push({ name: 'Login', query: { redirect: this.$route.fullPath } })
        return
      }

      try {
        await addToCart({ productId: product.id, quantity: 1 })
        this.$message.success(`已成功将「${product.name}」加入购物车`)
        this.$store.dispatch('fetchCartCount')
      } catch (e) {
        console.error('加入购物车失败:', e)
      }
    }
  }
}
</script>

<style scoped>
/* ✅ 搜索框样式 — 与下方分类栏等宽、无白底、融入页面 */
.search-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 25px;
}
/* 输入框拉长，占据绝大部分宽度，字号/内边距/圆角对齐分类按钮 */
.search-input {
  flex: 1;
  padding: 12px 24px;
  border: 2px solid var(--border-color, #ede6dc);
  border-radius: 25px;
  font-size: 0.95rem;
  outline: none;
  color: var(--text-color, #3d2b1a);
  background: var(--bg-light, #faf7f3);
  transition: border-color 0.2s;
  font-family: inherit;
}
.search-input:focus {
  border-color: var(--primary-color, #d4a76a);
  background: var(--white, #fff);
}
.search-input::placeholder {
  color: #c4b49e;
  font-size: 0.95rem;
}
/* 手机端：输入框和按钮竖向排列 */
@media (max-width: 768px) {
  .search-bar-wrapper {
    flex-direction: column;
    gap: 10px;
  }
}
</style>
